package me.marin.lockout.client;

import me.lucko.fabric.api.permissions.v0.Permissions;
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
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import me.marin.lockout.generator.GoalGroup;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;
import oshi.util.tuples.Pair;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static me.marin.lockout.Constants.*;

public class LockoutClient implements ClientModInitializer {

    public static Lockout lockout;
    public static boolean amIPlayingLockout = false;
    public static String currentBoardType = null; // Current board type for filtering goals
    public static java.util.List<String> currentExcludedGoals = new java.util.ArrayList<>(); // Excluded goals from server
    private static KeyMapping keyBinding;
    private static KeyMapping goalListKeyBinding;
    private static KeyMapping toggleBoardKeyBinding;
    private static KeyMapping toggleSectionViewKeyBinding;
    private static KeyMapping nextSectionKeyBinding;
    private static KeyMapping toggleAutoCycleSectionKeyBinding;
    public static boolean boardVisible = true;
    public static boolean lockoutDebugHudOpen = false;  // mirrors debug HUD open state, updated only on pure F3 (no combo)
    public static boolean sectionViewEnabled = false;
    public static int currentSection = 1;  // 1-4, which section to display
    public static boolean autoCycleSectionEnabled = false;
    public static int autoCycleSectionInterval = 60;  // Ticks between section changes
    public static int lastSectionCycleTime = 0;  // Tracks when the last section was cycled
    public static int CURRENT_TICK = 0;
    public static final Map<String, String> goalTooltipMap = new HashMap<>();

    public static final MenuType<BoardScreenHandler> BOARD_SCREEN_HANDLER;
    public static final KeyMapping.Category LOCKOUT_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "keybinds"));

    // Global cache for player skin textures (keyed by player name)
    public static final java.util.Map<String, net.minecraft.resources.Identifier> PLAYER_SKIN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        BOARD_SCREEN_HANDLER = new MenuType<>(BoardScreenHandler::new, FeatureFlags.VANILLA_SET);
    }

    private static boolean hasPermission() {
        // Allow all users to access client commands
        // The server will validate permissions when it receives the packets
        return true;
    }

    private static boolean hasPermissionLevel() {
        // Allow all users to access the goal list GUI
        // The server will validate permissions for actual actions
        return true;
    }

    public static List<KeyMapping> getLockoutKeyBindings() {
        List<KeyMapping> bindings = new ArrayList<>();
        if (keyBinding != null) bindings.add(keyBinding);
        if (goalListKeyBinding != null) bindings.add(goalListKeyBinding);
        if (toggleBoardKeyBinding != null) bindings.add(toggleBoardKeyBinding);
        if (toggleSectionViewKeyBinding != null) bindings.add(toggleSectionViewKeyBinding);
        if (nextSectionKeyBinding != null) bindings.add(nextSectionKeyBinding);
        if (toggleAutoCycleSectionKeyBinding != null) bindings.add(toggleAutoCycleSectionKeyBinding);
        return bindings;
    }

    private static void resetSectionViewState() {
        sectionViewEnabled = false;
        currentSection = 1;
        autoCycleSectionEnabled = false;
    }

    private static void sendBoardTypeMessage(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(message);
        }
    }

    @Override
    public void onInitializeClient() {
        Registry.register(BuiltInRegistries.MENU, Constants.BOARD_SCREEN_ID, BOARD_SCREEN_HANDLER);

        ClientPlayNetworking.registerGlobalReceiver(LockoutGoalsTeamsPayload.ID, (payload, context) -> {
            List<LockoutTeam> teams = payload.teams();

            LockoutClient.amIPlayingLockout = teams.stream().map(LockoutTeam::getPlayerNames)
                    .anyMatch(players -> players.stream().anyMatch(player -> player.equals(Minecraft.getInstance().getUser().getName())));

            int[] completedByTeam = payload.goals().stream().mapToInt(Pair::getB).toArray();

            lockout = new Lockout(new LockoutBoard(payload.goals().stream().map(Pair::getA).toList()), teams);
            lockout.setRunning(payload.isRunning());

            // Always reset section state on new board/game payload to avoid stale section lock-in.
            resetSectionViewState();

            List<Goal> goalList = lockout.getBoard().getGoals();
            for (int i = 0; i < goalList.size(); i++) {
                if (completedByTeam[i] != -1) {
                    LockoutTeam team = lockout.getTeams().get(completedByTeam[i]);
                    goalList.get(i).setCompleted(true, team);
                    team.addPoint();
                }
            }

            Minecraft client = context.client();
            client.execute(() -> {
                if (client.player != null) {
                    client.gui.setScreen(new BoardScreen(BOARD_SCREEN_HANDLER.create(0, client.player.getInventory()), client.player.getInventory(), Component.empty()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateTooltipPayload.ID, (payload, context) -> {
            goalTooltipMap.put(payload.goal(), payload.tooltip());
        });
        
        ClientPlayNetworking.registerGlobalReceiver(me.marin.lockout.network.GoalDetailsPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (client.player != null) {
                    // Display goal details in chat
                    String[] lines = payload.details().split("\\\\n");
                    for (String line : lines) {
                        client.player.sendSystemMessage(Component.literal(line));
                    }
                }
            });
        });
        
        ClientPlayNetworking.registerGlobalReceiver(me.marin.lockout.network.DownloadStatisticsPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                try {
                    // Create lockout-statistics directory in client's instance folder
                    java.nio.file.Path statisticsDir = client.gameDirectory.toPath().resolve("lockout-statistics");
                    if (!java.nio.file.Files.exists(statisticsDir)) {
                        java.nio.file.Files.createDirectories(statisticsDir);
                    }
                    
                    // Save the file
                    java.nio.file.Path statsFile = statisticsDir.resolve(payload.filename());
                    java.nio.file.Files.writeString(statsFile, payload.content());
                    
                    // Send success message with clickable link
                    if (client.player != null) {
                        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal("Statistics saved! ")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)
                            .append(
                                net.minecraft.network.chat.Component.literal("[Open File]")
                                    .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD)
                                    .withStyle(style -> style
                                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenFile(statsFile.toFile().getAbsolutePath()))
                                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(net.minecraft.network.chat.Component.literal("MouseButtonEvent to open: " + statsFile.toFile().getAbsolutePath())))
                                    )
                            );
                        client.player.sendSystemMessage(message);
                    }
                } catch (java.io.IOException e) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("Failed to save statistics file: " + e.getMessage())
                                .withStyle(net.minecraft.ChatFormatting.RED)
                        );
                    }
                }
            });
        });
        
        ClientPlayNetworking.registerGlobalReceiver(UpdatePicksBansPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
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
                if (client.gui.screen() instanceof GoalListScreen goalListScreen) {
                    goalListScreen.refreshPanels();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(BroadcastPickBanPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
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
                    
                    // Get the actual goal name from the goal instance
                    String goalName;
                    me.marin.lockout.lockout.Goal goal = me.marin.lockout.lockout.GoalRegistry.INSTANCE.newGoal(payload.goalId(), null);
                    if (goal != null) {
                        goalName = goal.getGoalName();
                    } else {
                        // Fallback to capitalized ID if goal creation fails
                        goalName = org.apache.commons.lang3.text.WordUtils.capitalize(
                            payload.goalId().replace("_", " ").toLowerCase(), ' '
                        );
                    }
                    
                    // Create and display the message
                    Component message;
                    if ("pick".equals(payload.action())) {
                        message = Component.literal(payload.playerName() + " has picked " + goalName + "!").withColor(0x55FF55);
                    } else if ("ban".equals(payload.action())) {
                        message = Component.literal(payload.playerName() + " has banned " + goalName + "!").withColor(0xFF5555);
                    } else if ("unpick".equals(payload.action())) {
                        message = Component.literal(payload.playerName() + " has unpicked " + goalName + ".").withStyle(net.minecraft.ChatFormatting.GRAY);
                    } else { // "unban"
                        message = Component.literal(payload.playerName() + " has unbanned " + goalName + ".").withStyle(net.minecraft.ChatFormatting.GRAY);
                    }
                    client.player.sendSystemMessage(message);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncPickBanLimitPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                GoalGroup.setCustomLimit(payload.limit());
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("Pick/Ban limit set to " + payload.limit()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SetBoardTypePayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                // Store the board type and excluded goals from the server
                currentBoardType = payload.boardType();
                currentExcludedGoals = payload.excludedGoals();
                
                // If PickBan GUI is open, refresh it
                if (client.gui.screen() instanceof GoalListScreen screen) {
                    screen.refreshFromBoardType();
                }
                
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("Board type updated to: " + payload.boardType() + " (" + currentExcludedGoals.size() + " goals excluded)"));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(me.marin.lockout.network.SyncLocateDataPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                // Update ClientLocateUtil with server-provided locate data
                ClientLocateUtil.setServerLocateData(payload.biomeLocateData(), payload.structureLocateData());
                
                // If PickBan GUI is open, refresh it to show newly available goals
                if (client.gui.screen() instanceof GoalListScreen screen) {
                    screen.init(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StartPickBanSessionPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
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
                client.gui.setScreen(new GoalListScreen());
                
                if (client.player != null) {
                    client.player.sendSystemMessage(
                        Component.literal("Pick/ban session started: " + payload.team1Name() + " vs " + payload.team2Name()).withColor(0x55FF55)
                    );
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdatePickBanSessionPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
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
                if (client.gui.screen() instanceof GoalListScreen goalListScreen) {
                    goalListScreen.refreshForPickBanSession(payload);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(EndPickBanSessionPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
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
                if (client.gui.screen() instanceof GoalListScreen) {
                    client.gui.setScreen(null);
                }
                
                if (client.player != null) {
                    if (payload.cancelled()) {
                        client.player.sendSystemMessage(
                            Component.literal("Pick/ban session has been cancelled by an admin.").withColor(0xFF5555)
                        );
                    }
                    // Note: Completion message is sent by server broadcast, not here
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StartLockoutPayload.ID, (payload, context) -> {
            // Null check to prevent crash when packet arrives before LockoutGoalsTeamsPayload
            if (lockout != null) {
                lockout.setStarted(true);
            }
            context.client().execute(() -> {
                if (Minecraft.getInstance().gui.screen() != null) {
                    Minecraft.getInstance().gui.screen().onClose();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(UpdateTimerPayload.ID, (payload, context) -> {
            // Null check to prevent crash when packet arrives before LockoutGoalsTeamsPayload
            if (lockout != null) {
                lockout.setTicks(payload.ticks());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(CompleteTaskPayload.ID, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                // Null check to prevent crash when packet arrives before LockoutGoalsTeamsPayload
                if (lockout == null) return;
                
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
                            client.player.playSound(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "block.note_block.chime")), 2f, 1f);
                        } else {
                            client.player.playSound(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.guardian.death")), 2f, 1f);
                        }
                    }
                }
                goal.setCompletedMessage(payload.completionMessage());


            });
        });
        ClientPlayNetworking.registerGlobalReceiver(EndLockoutPayload.ID, (payload, context) -> {
            // Null check to prevent crash when packet arrives before LockoutGoalsTeamsPayload
            if (lockout == null) return;
            
            lockout.setRunning(false);
            Minecraft client = context.client();
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
                        client.player.playSound(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.pillager.celebrate")), 2f, 1f);
                    } else {
                        client.player.playSound(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.warden.death")), 2f, 1f);
                    }
                }
            });
        });

        ArgumentTypeRegistry.registerArgumentType(Constants.BOARD_FILE_ARGUMENT_TYPE, CustomBoardFileArgumentType.class, SingletonArgumentInfo.contextFree(CustomBoardFileArgumentType::newInstance));
        ArgumentTypeRegistry.registerArgumentType(Constants.BOARD_POSITION_ARGUMENT_TYPE, BoardPositionArgumentType.class, SingletonArgumentInfo.contextFree(BoardPositionArgumentType::newInstance));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            {
                var commandNode = ClientCommands.literal("PickBanLimit").then(
                    ClientCommands.argument("limit", IntegerArgumentType.integer(0)).executes(ctx -> {
                        int limit = IntegerArgumentType.getInteger(ctx, "limit");
                        // Send to server so it can broadcast to all players
                        ClientPlayNetworking.send(new SyncPickBanLimitPayload(limit));
                        return 1;
                    })
                ).build();
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("BoardPosition").build();
                var positionNode = ClientCommands.argument("board position", BoardPositionArgumentType.newInstance()).executes((context) -> {
                    String position = context.getArgument("board position", String.class);

                    LockoutConfig.BoardPosition boardPosition = LockoutConfig.BoardPosition.match(position);
                    if (boardPosition == null) {
                        context.getSource().sendError(Component.literal("Invalid board position: " + position + "."));
                        return 0;
                    }
                    LockoutConfig.getInstance().boardPosition = boardPosition;
                    LockoutConfig.save();

                    context.getSource().sendFeedback(Component.literal("Updated board position." + (boardPosition == LockoutConfig.BoardPosition.LEFT ? " Note: Opening debug hud (F3) will hide the board." : "")));

                    return 1;
                }).build();

                dispatcher.getRoot().addChild(commandNode);
                commandNode.addChild(positionNode);
            }
            {
                var commandNode = ClientCommands.literal("BoardBuilder").executes((context) -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> {
                        if (client.player != null) {
                            client.gui.setScreen(new BoardBuilderScreen());
                        }
                    });

                    return 1;
                }).build();

                var boardNameNode = ClientCommands.argument("board name", CustomBoardFileArgumentType.newInstance()).executes((context) -> {
                    String boardName = context.getArgument("board name", String.class);

                    JSONBoard jsonBoard;
                    try {
                        jsonBoard = BoardBuilderIO.INSTANCE.readBoard(boardName);
                    } catch (IOException e) {
                        context.getSource().sendError(Component.literal("Error while trying to read board."));
                        return 0;
                    }

                    int size = (int) Math.sqrt(jsonBoard.goals.size());
                    if (size * size != jsonBoard.goals.size() || size < MIN_BOARD_SIZE || size > MAX_BOARD_SIZE) {
                        context.getSource().sendError(Component.literal("Board doesn't have a valid number of goals!"));
                        return 0;
                    }

                    List<Pair<String, String>> goals = jsonBoard.goals.stream()
                            .map(goal -> new Pair<>(goal.id, goal.data != null ? goal.data : GoalDataConstants.DATA_NONE)).toList();

                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> {
                        if (client.player != null) {
                            BoardBuilderData.INSTANCE.setBoard(boardName, size, goals);
                            client.gui.setScreen(new BoardBuilderScreen());
                        }
                    });

                    return 1;
                }).build();

                commandNode.addChild(boardNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("CreateBoardType")
                    .requires(ccs -> hasPermission())
                    .executes((context) -> {
                        Minecraft.getInstance().execute(() -> 
                            Minecraft.getInstance().gui.setScreen(new BoardTypeCreatorScreen()));
                        return 1;
                    }).build();

                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("EditBoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommands.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        Minecraft.getInstance().execute(() -> {
                            try {
                                JSONBoardType existingBoardType = BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                if (existingBoardType == null) {
                                    sendBoardTypeMessage(Component.literal("BoardType not found: ").withStyle(ChatFormatting.RED)
                                        .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW)));
                                    return;
                                }
                                Minecraft.getInstance().gui.setScreen(new BoardTypeCreatorScreen(existingBoardType));
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Component.literal("Failed to load BoardType: " + e.getMessage()).withStyle(ChatFormatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("ListBoardTypes")
                    .requires(ccs -> hasPermission())
                    .executes((context) -> {
                        Minecraft.getInstance().execute(() -> {
                            try {
                                List<String> customBoardTypes = BoardTypeIO.INSTANCE.getSavedBoardTypes();
                                
                                if (customBoardTypes.isEmpty()) {
                                    sendBoardTypeMessage(Component.literal("No custom BoardTypes found.").withStyle(ChatFormatting.YELLOW));
                                    return;
                                }

                                String storagePath = Minecraft.getInstance().gameDirectory.toPath().resolve("lockout-boardtypes").toString();
                                sendBoardTypeMessage(Component.literal("Storage location: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(storagePath).withStyle(ChatFormatting.AQUA)));
                                sendBoardTypeMessage(Component.literal("Custom BoardTypes (" + customBoardTypes.size() + "):").withStyle(ChatFormatting.GREEN));
                                
                                for (String boardTypeName : customBoardTypes) {
                                    try {
                                        JSONBoardType boardType = BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                        int excludedCount = boardType.excludedGoals != null ? boardType.excludedGoals.size() : 0;
                                        sendBoardTypeMessage(Component.literal("  - ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW))
                                            .append(Component.literal(" (" + excludedCount + " goals excluded)").withStyle(ChatFormatting.GRAY)));
                                    } catch (IOException e) {
                                        sendBoardTypeMessage(Component.literal("  - ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW)));
                                    }
                                }
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Component.literal("Failed to list BoardTypes: " + e.getMessage()).withStyle(ChatFormatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("BoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommands.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        Minecraft.getInstance().execute(() -> {
                            try {
                                me.marin.lockout.json.JSONBoardType boardType = me.marin.lockout.type.BoardTypeIO.INSTANCE.readBoardType(boardTypeName);
                                if (boardType == null) {
                                    sendBoardTypeMessage(Component.literal("BoardType not found: ").withStyle(ChatFormatting.RED)
                                        .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW)));
                                    return;
                                }
                                
                                java.util.List<String> excludedGoals = boardType.excludedGoals != null ? boardType.excludedGoals : new java.util.ArrayList<>();
                                
                                // Send to server
                                ClientPlayNetworking.send(new me.marin.lockout.network.UploadBoardTypePayload(boardTypeName, excludedGoals));
                                
                                sendBoardTypeMessage(Component.literal("Board type set to '").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal("' (" + excludedGoals.size() + " goals excluded).").withStyle(ChatFormatting.GREEN)));
                            } catch (IOException e) {
                                Lockout.error(e);
                                sendBoardTypeMessage(Component.literal("Failed to load BoardType: " + e.getMessage()).withStyle(ChatFormatting.RED));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("DeleteBoardType")
                    .requires(ccs -> hasPermission())
                    .build();

                var boardTypeNameNode = ClientCommands.argument("board type name", CustomBoardTypeArgumentType.newInstance())
                    .executes((context) -> {
                        String boardTypeName = context.getArgument("board type name", String.class);

                        Minecraft.getInstance().execute(() -> {
                            boolean success = BoardTypeIO.INSTANCE.deleteBoardType(boardTypeName);
                            
                            if (success) {
                                BoardTypeManager.INSTANCE.clearCache();
                                sendBoardTypeMessage(Component.literal("Deleted custom BoardType: ").withStyle(ChatFormatting.GREEN)
                                    .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW)));
                            } else {
                                sendBoardTypeMessage(Component.literal("Failed to delete BoardType: ").withStyle(ChatFormatting.RED)
                                    .append(Component.literal(boardTypeName).withStyle(ChatFormatting.YELLOW)));
                            }
                        });
                        return 1;
                    }).build();

                commandNode.addChild(boardTypeNameNode);
                dispatcher.getRoot().addChild(commandNode);
            }
            {
                var commandNode = ClientCommands.literal("SetCustomBoard").requires(ccs -> hasPermission()).build();

                var boardNameNode = ClientCommands.argument("board name", CustomBoardFileArgumentType.newInstance()).executes((context) -> {
                    String boardName = context.getArgument("board name", String.class);

                    JSONBoard jsonBoard;
                    try {
                        jsonBoard = BoardBuilderIO.INSTANCE.readBoard(boardName);
                    } catch (IOException e) {
                        context.getSource().sendError(Component.literal("Error while trying to read board."));
                        return 0;
                    }

                    int size = (int) Math.sqrt(jsonBoard.goals.size());
                    if (size * size != jsonBoard.goals.size() || size < MIN_BOARD_SIZE || size > MAX_BOARD_SIZE) {
                        context.getSource().sendError(Component.literal("Board doesn't have a valid number of goals!"));
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
                Minecraft.getInstance().player.connection.getConnection().disconnect(Component.literal("Wrong Lockout version: v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + ".\nServer is using Lockout v" + version + "."));
                return;
            }

            // Respond with version, it will be compared on server as well
            ClientPlayNetworking.send(new LockoutVersionPayload(LockoutInitializer.MOD_VERSION.getFriendlyString()));
        });

        keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.open_board", // The translation key of the keybinding's name
            InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_B, // The keycode of the key
            LOCKOUT_CATEGORY // The translation key of the keybinding's category.
        ));

        goalListKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.open_goal_list",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            LOCKOUT_CATEGORY // The translation key of the keybinding's category.
        ));

        toggleBoardKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.toggle_board",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            LOCKOUT_CATEGORY
        ));

        toggleSectionViewKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.toggle_section_view",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            LOCKOUT_CATEGORY
        ));

        nextSectionKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.next_section",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            LOCKOUT_CATEGORY
        ));

        toggleAutoCycleSectionKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.lockout.toggle_auto_cycle_section",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            LOCKOUT_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CURRENT_TICK++;

            boolean wasPressed = false;
            while (keyBinding.consumeClick()) {
                wasPressed = true;
            }
            if (wasPressed) {
                if (client.gui.screen() != null || client.player == null) {
                    return;
                }

                // If the game hasn't started, open board builder instead
                if (!Lockout.exists(lockout)) {
                    client.gui.setScreen(new BoardBuilderScreen());
                    return;
                }

                // Open GUI
                client.gui.setScreen(new BoardScreen(BOARD_SCREEN_HANDLER.create(0, client.player.getInventory()), client.player.getInventory(), Component.empty()));
            }

            boolean goalListPressed = false;
            while (goalListKeyBinding.consumeClick()) {
                goalListPressed = true;
            }
            if (goalListPressed) {
                if (client.gui.screen() != null || client.player == null) {
                    return;
                }
                // Allow anyone to open during active pick/ban session, otherwise only operators
                boolean hasActiveSession = ClientPickBanSessionHolder.getActiveSession() != null;
                if (!hasActiveSession && !hasPermissionLevel()) {
                    client.player.sendSystemMessage(Component.literal("You must be an operator to access the Goal List!").withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
                client.gui.setScreen(new GoalListScreen());
            }

            boolean toggleBoardPressed = false;
            while (toggleBoardKeyBinding.consumeClick()) {
                toggleBoardPressed = true;
            }
            if (toggleBoardPressed) {
                // Toggle board visibility (client-side only, no packet sent)
                boardVisible = !boardVisible;
            }

            boolean toggleSectionViewPressed = false;
            while (toggleSectionViewKeyBinding.consumeClick()) {
                toggleSectionViewPressed = true;
            }
            if (toggleSectionViewPressed) {
                if (client.gui.screen() != null || client.player == null) {
                    return;
                }
                if (!Lockout.exists(LockoutClient.lockout)) {
                    return;
                }

                // Always allow disabling section mode, regardless of board size.
                if (sectionViewEnabled) {
                    resetSectionViewState();
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("Section view disabled").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                    return;
                }

                // Only allow enabling if board is at least 4x4.
                if (LockoutClient.lockout.getBoard().size() < 4) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("Board is too small to use section view").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                    return;
                }

                // Enable section view on section 1.
                sectionViewEnabled = true;
                currentSection = 1;

                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("Section view enabled: Section 1").withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            }

            boolean nextSectionPressed = false;
            while (nextSectionKeyBinding.consumeClick()) {
                nextSectionPressed = true;
            }
            if (nextSectionPressed) {
                if (client.gui.screen() != null || client.player == null) {
                    return;
                }
                
                // Only cycle sections if section view is enabled
                if (sectionViewEnabled) {
                    currentSection = currentSection % 4 + 1;  // Cycle 1→2→3→4→1
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("Section " + currentSection).withStyle(net.minecraft.ChatFormatting.GREEN));
                    }
                }
            }

            boolean toggleAutoCyclePressed = false;
            while (toggleAutoCycleSectionKeyBinding.consumeClick()) {
                toggleAutoCyclePressed = true;
            }
            if (toggleAutoCyclePressed) {
                if (client.gui.screen() != null || client.player == null) {
                    return;
                }
                if (!Lockout.exists(LockoutClient.lockout)) {
                    return;
                }
                
                // Only allow if section view is enabled
                if (!sectionViewEnabled) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("Enable section view first").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                    return;
                }
                
                // Toggle auto-cycling on/off
                autoCycleSectionEnabled = !autoCycleSectionEnabled;
                lastSectionCycleTime = CURRENT_TICK;
                
                if (autoCycleSectionEnabled) {
                    if (client.player != null) {
                        client.player.sendSystemMessage(
                            Component.literal("Auto-cycling enabled (").withStyle(net.minecraft.ChatFormatting.GREEN)
                                .append(Component.literal(String.valueOf(autoCycleSectionInterval / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW))
                                .append(Component.literal("s interval)").withStyle(net.minecraft.ChatFormatting.GREEN))
                        );
                    }
                } else {
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("Auto-cycling disabled").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    }
                }
            }

            // Auto-cycle sections if enabled
            if (sectionViewEnabled && autoCycleSectionEnabled) {
                if (CURRENT_TICK - lastSectionCycleTime >= autoCycleSectionInterval) {
                    currentSection = currentSection % 4 + 1;  // Cycle 1→2→3→4→1
                    lastSectionCycleTime = CURRENT_TICK;
                }
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LockoutConfig.load(); // reload config every time player joins world
            resetSectionViewState();
            // Load auto-cycle settings from config
            autoCycleSectionEnabled = LockoutConfig.getInstance().autoCycleSectionsEnabled;
            autoCycleSectionInterval = LockoutConfig.getInstance().autoCycleInterval;
            lastSectionCycleTime = CURRENT_TICK;
            // Build locate cache once on join so opening the goal list doesn't trigger expensive searches
            client.execute(() -> ClientLocateUtil.buildCacheFromRegisteredGoals(client));
        });
        ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
            lockout = null;
            resetSectionViewState();
            goalTooltipMap.clear();
            ClientLocateUtil.clearCache();
        }));

        MenuScreens.register(BOARD_SCREEN_HANDLER, BoardScreen::new);
    }

}
