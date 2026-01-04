package me.marin.lockout.client;

import me.marin.lockout.*;
import me.marin.lockout.client.gui.*;
import me.marin.lockout.json.JSONBoard;
import me.marin.lockout.json.JSONBoardType;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.network.*;
import me.marin.lockout.type.BoardTypeIO;
import me.marin.lockout.type.BoardTypeManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import me.marin.lockout.generator.GoalGroup;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import oshi.util.tuples.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static me.marin.lockout.Constants.MAX_BOARD_SIZE;
import static me.marin.lockout.Constants.MIN_BOARD_SIZE;

public class LockoutClient implements ClientModInitializer {

    public static Lockout lockout;
    public static boolean amIPlayingLockout = false;
    public static String currentBoardType = null; // Current board type for filtering goals
    public static java.util.List<String> currentExcludedGoals = new java.util.ArrayList<>(); // Excluded goals from server
    private static KeyBinding keyBinding;
    private static KeyBinding goalListKeyBinding;
    public static int CURRENT_TICK = 0;
    public static final Map<String, String> goalTooltipMap = new HashMap<>();

    public static final ScreenHandlerType<BoardScreenHandler> BOARD_SCREEN_HANDLER;

    static {
        BOARD_SCREEN_HANDLER = new ScreenHandlerType<>(BoardScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
    }

    private static boolean hasPermission() {
        return MinecraftClient.getInstance().isInSingleplayer() || 
               MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.hasPermissionLevel(2);
    }

    private static void sendBoardTypeMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(message, false);
        }
    }

    @Override
    public void onInitializeClient() {
        Registry.register(Registries.SCREEN_HANDLER, Constants.BOARD_SCREEN_ID, BOARD_SCREEN_HANDLER);

        ClientPlayNetworking.registerGlobalReceiver(LockoutGoalsTeamsPayload.ID, (payload, context) -> {
            List<LockoutTeam> teams = payload.teams();

            LockoutClient.amIPlayingLockout = teams.stream().map(LockoutTeam::getPlayerNames)
                    .anyMatch(players -> players.stream().anyMatch(player -> player.equals(MinecraftClient.getInstance().getSession().getUsername())));

            int[] completedByTeam = payload.goals().stream().mapToInt(Pair::getB).toArray();

            lockout = new Lockout(new LockoutBoard(payload.goals().stream().map(Pair::getA).toList()), teams);
            lockout.setRunning(payload.isRunning());

            List<Goal> goalList = lockout.getBoard().getGoals();
            for (int i = 0; i < goalList.size(); i++) {
                if (completedByTeam[i] != -1) {
                    LockoutTeam team = lockout.getTeams().get(completedByTeam[i]);
                    goalList.get(i).setCompleted(true, team);
                    team.addPoint();
                }
            }

            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.player != null) {
                    client.setScreen(new BoardScreen(BOARD_SCREEN_HANDLER.create(0, client.player.getInventory()), client.player.getInventory(), Text.empty()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateTooltipPayload.ID, (payload, context) -> {
            goalTooltipMap.put(payload.goal(), payload.tooltip());
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdatePicksBansPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                // During pick/ban session, the payload contains combined PICKS + PENDING_PICKS
                // We need to separate locked goals from pending goals
                List<String> lockedPicks = new ArrayList<>(GoalGroup.PICKS.getGoals());
                List<String> lockedBans = new ArrayList<>(GoalGroup.BANS.getGoals());
                
                // Clear and update PICKS (locked goals only)
                GoalGroup.PICKS.getGoals().clear();
                GoalGroup.PICKS.getGoals().addAll(lockedPicks);
                GoalGroup.BANS.getGoals().clear();
                GoalGroup.BANS.getGoals().addAll(lockedBans);
                
                // Update PENDING lists with new selections (excluding already locked goals)
                GoalGroup.PENDING_PICKS.getGoals().clear();
                for (String pick : payload.picks()) {
                    if (!lockedPicks.contains(pick)) {
                        GoalGroup.PENDING_PICKS.getGoals().add(pick);
                    }
                }
                
                GoalGroup.PENDING_BANS.getGoals().clear();
                for (String ban : payload.bans()) {
                    if (!lockedBans.contains(ban)) {
                        GoalGroup.PENDING_BANS.getGoals().add(ban);
                    }
                }
                
                // If the payload has empty picks/bans, clear everything
                if (payload.picks().isEmpty() && payload.bans().isEmpty()) {
                    GoalGroup.PENDING_PICKS.getGoals().clear();
                    GoalGroup.PENDING_BANS.getGoals().clear();
                }
                
                // Update goal-to-player mapping
                GoalGroup.clearGoalPlayers();
                for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                    GoalGroup.setGoalPlayer(entry.getKey(), entry.getValue());
                }
                
                // Refresh the GUI if it's open
                if (client.currentScreen instanceof GoalListScreen goalListScreen) {
                    goalListScreen.refreshPanels();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(BroadcastPickBanPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.player != null) {
                    // Only update the goal player mapping for other players
                    // The actual list syncing is handled by UpdatePicksBansPayload
                    boolean isFromCurrentPlayer = payload.playerName().equals(client.player.getName().getString());
                    
                    if (!isFromCurrentPlayer) {
                        if ("pick".equals(payload.action()) || "ban".equals(payload.action())) {
                            GoalGroup.setGoalPlayer(payload.goalId(), payload.playerName());
                        }
                    }
                    
                    // Format the goal name
                    String goalName = org.apache.commons.lang3.text.WordUtils.capitalize(
                        payload.goalId().replace("_", " ").toLowerCase(), ' '
                    );
                    
                    // Create and display the message
                    Text message;
                    if ("pick".equals(payload.action())) {
                        message = Text.literal(payload.playerName() + " has picked " + goalName + "!").withColor(0x55FF55);
                    } else if ("ban".equals(payload.action())) {
                        message = Text.literal(payload.playerName() + " has banned " + goalName + "!").withColor(0xFF5555);
                    } else if ("unpick".equals(payload.action())) {
                        message = Text.literal(payload.playerName() + " has unpicked " + goalName + ".").formatted(net.minecraft.util.Formatting.GRAY);
                    } else { // "unban"
                        message = Text.literal(payload.playerName() + " has unbanned " + goalName + ".").formatted(net.minecraft.util.Formatting.GRAY);
                    }
                    client.player.sendMessage(message, false);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncPickBanLimitPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                GoalGroup.setCustomLimit(payload.limit());
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Pick/Ban limit set to " + payload.limit()), false);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SetBoardTypePayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                // Store the board type and excluded goals from the server
                currentBoardType = payload.boardType();
                currentExcludedGoals = payload.excludedGoals();
                
                // If PickBan GUI is open, refresh it
                if (client.currentScreen instanceof GoalListScreen screen) {
                    screen.refreshFromBoardType();
                }
                
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Board type updated to: " + payload.boardType() + " (" + currentExcludedGoals.size() + " goals excluded)"), false);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StartPickBanSessionPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                // Clear all goal groups before starting the session
                GoalGroup.PICKS.getGoals().clear();
                GoalGroup.BANS.getGoals().clear();
                GoalGroup.PENDING_PICKS.getGoals().clear();
                GoalGroup.PENDING_BANS.getGoals().clear();
                GoalGroup.clearGoalPlayers();
                
                // Create initial session state
                UpdatePickBanSessionPayload initialState = new UpdatePickBanSessionPayload(
                    1, // currentRound
                    true, // isTeam1Turn
                    payload.team1Name(),
                    payload.team2Name(),
                    new java.util.HashSet<>(), // allLockedPicks
                    new java.util.HashSet<>(), // allLockedBans
                    new java.util.ArrayList<>(), // pendingPicks
                    new java.util.ArrayList<>(), // pendingBans
                    payload.selectionLimit(),
                    new java.util.HashMap<>(), // goalToPlayerMap
                    3 // maxRounds - default, will be updated by server if different
                );
                ClientPickBanSessionHolder.setActiveSession(initialState);
                
                // Open the pick/ban GUI for players
                client.setScreen(new GoalListScreen());
                
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("Pick/ban session started: " + payload.team1Name() + " vs " + payload.team2Name()).withColor(0x55FF55),
                        false
                    );
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdatePickBanSessionPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                // Update client-side session state
                ClientPickBanSessionHolder.setActiveSession(payload);
                
                // Update client-side pick/ban lists to show locked goals BEFORE refreshing GUI
                GoalGroup.PICKS.getGoals().clear();
                GoalGroup.PICKS.getGoals().addAll(payload.allLockedPicks());
                GoalGroup.BANS.getGoals().clear();
                GoalGroup.BANS.getGoals().addAll(payload.allLockedBans());
                
                // Update goal-to-player mapping from payload
                GoalGroup.clearGoalPlayers();
                for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                    GoalGroup.setGoalPlayer(entry.getKey(), entry.getValue());
                }
                
                // Clear PENDING goal groups to prevent duplicates and reset limit
                GoalGroup.PENDING_PICKS.getGoals().clear();
                GoalGroup.PENDING_BANS.getGoals().clear();
                
                // Update the GUI if it's open (now panels will have updated picks/bans)
                if (client.currentScreen instanceof GoalListScreen goalListScreen) {
                    goalListScreen.refreshForPickBanSession(payload);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(EndPickBanSessionPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (payload.cancelled()) {
                    // Clear all goal groups when cancelled
                    GoalGroup.PICKS.getGoals().clear();
                    GoalGroup.BANS.getGoals().clear();
                    GoalGroup.PENDING_PICKS.getGoals().clear();
                    GoalGroup.PENDING_BANS.getGoals().clear();
                    GoalGroup.clearGoalPlayers();
                } else {
                    // Update final picks/bans if session completed normally
                    GoalGroup.PICKS.getGoals().clear();
                    GoalGroup.PICKS.getGoals().addAll(payload.finalPicks());
                    GoalGroup.BANS.getGoals().clear();
                    GoalGroup.BANS.getGoals().addAll(payload.finalBans());
                    
                    // Update goal-to-player mapping
                    GoalGroup.clearGoalPlayers();
                    for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                        GoalGroup.setGoalPlayer(entry.getKey(), entry.getValue());
                    }
                    
                    // Clear PENDING goal groups
                    GoalGroup.PENDING_PICKS.getGoals().clear();
                    GoalGroup.PENDING_BANS.getGoals().clear();
                }
                
                // Clear client-side session state
                ClientPickBanSessionHolder.clearSession();
                
                // Close the GUI if open
                if (client.currentScreen instanceof GoalListScreen) {
                    client.setScreen(null);
                }
                
                if (client.player != null) {
                    if (payload.cancelled()) {
                        client.player.sendMessage(
                            Text.literal("Pick/ban session has been cancelled by an admin.").withColor(0xFF5555),
                            false
                        );
                    }
                    // Note: Completion message is sent by server broadcast, not here
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StartLockoutPayload.ID, (payload, context) -> {
            lockout.setStarted(true);
            context.client().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen != null) {
                    MinecraftClient.getInstance().currentScreen.close();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateTimerPayload.ID, (payload, context) -> {
            lockout.setTicks(payload.ticks());
        });
        ClientPlayNetworking.registerGlobalReceiver(CompleteTaskPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                Goal goal = lockout.getBoard().getGoals().stream().filter(g -> g.getId().equals(payload.goal())).findFirst().get();
                if (goal.isCompleted() || payload.teamIndex() == -1) {
                    lockout.clearGoalCompletion(goal, false);
                }
                if (payload.teamIndex() != -1) {
                    LockoutTeam team = lockout.getTeams().get(payload.teamIndex());
                    team.addPoint();
                    goal.setCompleted(true, lockout.getTeams().get(payload.teamIndex()));

                    if (client.player != null && amIPlayingLockout) {
                        if (team.getPlayerNames().contains(client.player.getName().getString())) {
                            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 2f, 1f);
                        } else {
                            client.player.playSound(SoundEvents.ENTITY_GUARDIAN_DEATH, 2f, 1f);
                        }
                    }
                }
                goal.setCompletedMessage(payload.completionMessage());


            });
        });
        ClientPlayNetworking.registerGlobalReceiver(EndLockoutPayload.ID, (payload, context) -> {
            lockout.setRunning(false);
            MinecraftClient client = context.client();
            client.execute(() -> {
                if (client.player != null) {
                    boolean didIWin = false;
                    for (int winner : payload.winners()) {
                        LockoutTeam team = lockout.getTeams().get(winner);

                        if (team.getPlayerNames().contains(client.player.getName().getString())) {
                            didIWin = true;
                            break;
                        }
                    }
                    if (didIWin) {
                        client.player.playSound(SoundEvents.ENTITY_PILLAGER_CELEBRATE, 2f, 1f);
                    } else {
                        client.player.playSound(SoundEvents.ENTITY_WARDEN_DEATH, 2f, 1f);
                    }
                }
            });
        });

        ArgumentTypeRegistry.registerArgumentType(Constants.BOARD_FILE_ARGUMENT_TYPE, CustomBoardFileArgumentType.class, ConstantArgumentSerializer.of(CustomBoardFileArgumentType::newInstance));
        ArgumentTypeRegistry.registerArgumentType(Constants.BOARD_POSITION_ARGUMENT_TYPE, BoardPositionArgumentType.class, ConstantArgumentSerializer.of(BoardPositionArgumentType::newInstance));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            {
                var commandNode = ClientCommandManager.literal("PickBanLimit").then(
                    ClientCommandManager.argument("limit", IntegerArgumentType.integer(0)).executes(ctx -> {
                        int limit = IntegerArgumentType.getInteger(ctx, "limit");
                        // Send to server so it can broadcast to all players
                        ClientPlayNetworking.send(new SyncPickBanLimitPayload(limit));
                        return 1;
                    })
                ).build();
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("BoardPosition").build();
                var positionNode = ClientCommandManager.argument("board position", BoardPositionArgumentType.newInstance()).executes((context) -> {
                    String position = context.getArgument("board position", String.class);

                    LockoutConfig.BoardPosition boardPosition = LockoutConfig.BoardPosition.match(position);
                    if (boardPosition == null) {
                        context.getSource().sendError(Text.literal("Invalid board position: " + position + "."));
                        return 0;
                    }
                    LockoutConfig.getInstance().boardPosition = boardPosition;
                    LockoutConfig.save();

                    context.getSource().sendFeedback(Text.literal("Updated board position." + (boardPosition == LockoutConfig.BoardPosition.LEFT ? " Note: Opening debug hud (F3) will hide the board." : "")));

                    return 1;
                }).build();

                dispatcher.getRoot().addChild(commandNode);
                commandNode.addChild(positionNode);
            }
            {
                var commandNode = ClientCommandManager.literal("BoardBuilder").executes((context) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.send(() -> {
                        if (client.player != null) {
                            client.setScreen(new BoardBuilderScreen());
                        }
                    });

                    return 1;
                }).build();

                var boardNameNode = ClientCommandManager.argument("board name", CustomBoardFileArgumentType.newInstance()).executes((context) -> {
                    String boardName = context.getArgument("board name", String.class);

                    JSONBoard jsonBoard;
                    try {
                        jsonBoard = BoardBuilderIO.INSTANCE.readBoard(boardName);
                    } catch (IOException e) {
                        context.getSource().sendError(Text.literal("Error while trying to read board."));
                        return 0;
                    }

                    int size = (int) Math.sqrt(jsonBoard.goals.size());
                    if (size * size != jsonBoard.goals.size() || size < MIN_BOARD_SIZE || size > MAX_BOARD_SIZE) {
                        context.getSource().sendError(Text.literal("Board doesn't have a valid number of goals!"));
                        return 0;
                    }

                    List<Pair<String, String>> goals = jsonBoard.goals.stream()
                            .map(goal -> new Pair<>(goal.id, goal.data != null ? goal.data : GoalDataConstants.DATA_NONE)).toList();

                    MinecraftClient client = MinecraftClient.getInstance();
                    client.send(() -> {
                        if (client.player != null) {
                            BoardBuilderData.INSTANCE.setBoard(boardName, size, goals);
                            client.setScreen(new BoardBuilderScreen());
                        }
                    });

                    return 1;
                }).build();

                commandNode.addChild(boardNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("CreateBoardType")
                    .requires(ccs -> hasPermission())
                    .executes((context) -> {
                        MinecraftClient.getInstance().send(() -> 
                            MinecraftClient.getInstance().setScreen(new BoardTypeCreatorScreen()));
                        return 1;
                    }).build();

                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("EditBoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommandManager.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        MinecraftClient.getInstance().send(() -> {
                            try {
                                JSONBoardType existingBoardType = BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                if (existingBoardType == null) {
                                    sendBoardTypeMessage(Text.literal("BoardType not found: ").formatted(Formatting.RED)
                                        .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW)));
                                    return;
                                }
                                MinecraftClient.getInstance().setScreen(new BoardTypeCreatorScreen(existingBoardType));
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Text.literal("Failed to load BoardType: " + e.getMessage()).formatted(Formatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("ListBoardTypes")
                    .requires(ccs -> hasPermission())
                    .executes((context) -> {
                        MinecraftClient.getInstance().send(() -> {
                            try {
                                List<String> customBoardTypes = BoardTypeIO.INSTANCE.getSavedBoardTypes();
                                
                                if (customBoardTypes.isEmpty()) {
                                    sendBoardTypeMessage(Text.literal("No custom BoardTypes found.").formatted(Formatting.YELLOW));
                                    return;
                                }

                                String storagePath = MinecraftClient.getInstance().runDirectory.toPath().resolve("lockout-boardtypes").toString();
                                sendBoardTypeMessage(Text.literal("Storage location: ").formatted(Formatting.GRAY)
                                    .append(Text.literal(storagePath).formatted(Formatting.AQUA)));
                                sendBoardTypeMessage(Text.literal("Custom BoardTypes (" + customBoardTypes.size() + "):").formatted(Formatting.GREEN));
                                
                                for (String boardTypeName : customBoardTypes) {
                                    try {
                                        JSONBoardType boardType = BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                        int excludedCount = boardType.excludedGoals != null ? boardType.excludedGoals.size() : 0;
                                        sendBoardTypeMessage(Text.literal("  - ").formatted(Formatting.GRAY)
                                            .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW))
                                            .append(Text.literal(" (" + excludedCount + " goals excluded)").formatted(Formatting.GRAY)));
                                    } catch (IOException e) {
                                        sendBoardTypeMessage(Text.literal("  - ").formatted(Formatting.GRAY)
                                            .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW)));
                                    }
                                }
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Text.literal("Failed to list BoardTypes: " + e.getMessage()).formatted(Formatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("BoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommandManager.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        MinecraftClient.getInstance().send(() -> {
                            try {
                                me.marin.lockout.json.JSONBoardType boardType = me.marin.lockout.type.BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                if (boardType == null) {
                                    sendBoardTypeMessage(Text.literal("BoardType not found: ").formatted(Formatting.RED)
                                        .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW)));
                                    return;
                                }
                                
                                java.util.List<String> excludedGoals = boardType.excludedGoals != null ? boardType.excludedGoals : new java.util.ArrayList<>();
                                
                                // Send to server
                                ClientPlayNetworking.send(new me.marin.lockout.network.UploadBoardTypePayload(boardTypeName, excludedGoals));
                                
                                sendBoardTypeMessage(Text.literal("Board type set to '").formatted(Formatting.GREEN)
                                    .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW))
                                    .append(Text.literal("' (" + excludedGoals.size() + " goals excluded).").formatted(Formatting.GREEN)));
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Text.literal("Failed to load BoardType: " + e.getMessage()).formatted(Formatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("DeleteBoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommandManager.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        MinecraftClient.getInstance().send(() -> {
                            boolean success = BoardTypeIO.INSTANCE.deleteBoardType(boardTypeName);
                            
                            if (success) {
                                BoardTypeManager.INSTANCE.clearCache();
                                sendBoardTypeMessage(Text.literal("Deleted custom BoardType: ").formatted(Formatting.GREEN)
                                    .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW)));
                            } else {
                                sendBoardTypeMessage(Text.literal("Failed to delete BoardType: ").formatted(Formatting.RED)
                                    .append(Text.literal(boardTypeName).formatted(Formatting.YELLOW)));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommandManager.literal("SetCustomBoard").requires(ccs -> {
                    if (MinecraftClient.getInstance().isInSingleplayer()) {
                        return true;
                    }
                    return ccs.getPlayer().hasPermissionLevel(2);
                }).build();

                var boardNameNode = ClientCommandManager.argument("board name", CustomBoardFileArgumentType.newInstance()).executes((context) -> {
                    String boardName = context.getArgument("board name", String.class);

                    JSONBoard jsonBoard;
                    try {
                        jsonBoard = BoardBuilderIO.INSTANCE.readBoard(boardName);
                    } catch (IOException e) {
                        context.getSource().sendError(Text.literal("Error while trying to read board."));
                        return 0;
                    }

                    int size = (int) Math.sqrt(jsonBoard.goals.size());
                    if (size * size != jsonBoard.goals.size() || size < MIN_BOARD_SIZE || size > MAX_BOARD_SIZE) {
                        context.getSource().sendError(Text.literal("Board doesn't have a valid number of goals!"));
                        return 0;
                    }

                    ClientPlayNetworking.send(new CustomBoardPayload(Optional.of(jsonBoard.goals.stream()
                            .map(goal -> new Pair<>(goal.id, goal.data != null ? goal.data : GoalDataConstants.DATA_NONE)).toList())));
                    return 1;
                }).build();

                commandNode.addChild(boardNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(LockoutVersionPayload.ID, (payload, context) -> {
            // Compare Lockout versions, disconnect if invalid.
            String version = payload.version();
            if (!version.equals(LockoutInitializer.MOD_VERSION.getFriendlyString())) {
                MinecraftClient.getInstance().player.networkHandler.getConnection().disconnect(Text.of("Wrong Lockout version: v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + ".\nServer is using Lockout v" + version + "."));
                return;
            }

            // Respond with version, it will be compared on server as well
            ClientPlayNetworking.send(new LockoutVersionPayload(LockoutInitializer.MOD_VERSION.getFriendlyString()));
        });

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lockout.open_board", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_B, // The keycode of the key
                "category.lockout.keybinds" // The translation key of the keybinding's category.
        ));

        goalListKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lockout.open_goal_list",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.lockout.keybinds"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CURRENT_TICK++;

            boolean wasPressed = false;
            while (keyBinding.wasPressed()) {
                wasPressed = true;
            }
            if (wasPressed) {
                if (client.currentScreen != null || client.player == null) {
                    return;
                }

                // If the game hasn't started, open board builder instead
                if (!Lockout.exists(lockout)) {
                    client.setScreen(new BoardBuilderScreen());
                    return;
                }

                // Open GUI
                client.setScreen(new BoardScreen(BOARD_SCREEN_HANDLER.create(0, client.player.getInventory()), client.player.getInventory(), Text.empty()));
            }

            boolean goalListPressed = false;
            while (goalListKeyBinding.wasPressed()) {
                goalListPressed = true;
            }
            if (goalListPressed) {
                if (client.currentScreen != null || client.player == null) {
                    return;
                }
                // Allow anyone to open during active pick/ban session, otherwise only operators
                boolean hasActiveSession = ClientPickBanSessionHolder.getActiveSession() != null;
                if (!hasActiveSession && !client.player.hasPermissionLevel(2)) {
                    client.player.sendMessage(Text.literal("You must be an operator to access the Goal List!").formatted(net.minecraft.util.Formatting.RED), false);
                    return;
                }
                client.setScreen(new GoalListScreen());
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LockoutConfig.load(); // reload config every time player joins world
            // Build locate cache once on join so opening the goal list doesn't trigger expensive searches
            client.execute(() -> ClientLocateUtil.buildCacheFromRegisteredGoals(client));
        });
        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            lockout = null;
            goalTooltipMap.clear();
            ClientLocateUtil.clearCache();
        }));

        HandledScreens.register(BOARD_SCREEN_HANDLER, BoardScreen::new);
    }

}
