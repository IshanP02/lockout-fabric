package me.marin.lockout.server;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marin.lockout.*;
import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.generator.BoardGenerator;
import me.marin.lockout.generator.GoalGroup;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.network.AnnounceGoalFocusPayload;
import me.marin.lockout.network.BroadcastPickBanPayload;
import me.marin.lockout.network.CustomBoardPayload;
import me.marin.lockout.network.EndPickBanSessionPayload;
import me.marin.lockout.network.LockPickBanSelectionsPayload;
import me.marin.lockout.network.LockoutVersionPayload;
import me.marin.lockout.network.SetBoardTypePayload;
import me.marin.lockout.network.StartLockoutPayload;
import me.marin.lockout.network.StartPickBanSessionPayload;
import me.marin.lockout.network.SyncPickBanLimitPayload;
import me.marin.lockout.network.UpdatePickBanSessionPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import me.marin.lockout.network.UpdateTooltipPayload;
import me.marin.lockout.network.UploadBoardTypePayload;
import me.marin.lockout.server.handlers.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.AdvancementCommand;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import oshi.util.tuples.Pair;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.permission.LeveledPermissionPredicate;

import java.util.*;

import static me.marin.lockout.Constants.PLACEHOLDER_PERM_STRING;

public class LockoutServer {

    public static final int LOCATE_SEARCH = 750;
    public static final Map<RegistryKey<Biome>, LocateData> BIOME_LOCATE_DATA = new HashMap<>();
    public static final Map<RegistryKey<Structure>, LocateData> STRUCTURE_LOCATE_DATA = new HashMap<>();
    public static final List<DyeColor> AVAILABLE_DYE_COLORS = new ArrayList<>();

    private static int lockoutStartTime = 60;
    private static int boardSize;
    public static String boardType;
    public static java.util.List<String> boardTypeExcludedGoals = new java.util.ArrayList<>();

    // Cooldown system for goal pings
    private static final int MAX_PINGS = 4;
    private static final long COOLDOWN_WINDOW_MS = 15000; // 15 seconds
    private static final Map<UUID, List<Long>> playerPingTimestamps = new HashMap<>();

    public static Lockout lockout;
    public static MinecraftServer server;
    public static CompassItemHandler compassHandler;

    public static final Map<LockoutRunnable, Long> gameStartRunnables = new HashMap<>();

    private static LockoutBoard CUSTOM_BOARD = null;
    
    // Server-side picks and bans storage
    public static final List<String> SERVER_PICKS = new ArrayList<>();
    public static final List<String> SERVER_BANS = new ArrayList<>();
    public static final Map<String, String> SERVER_GOAL_TO_PLAYER_MAP = new HashMap<>();
    
    // Active pick/ban session
    public static PickBanSession activePickBanSession = null;
    
    // Default pick/ban limit (can be set before starting a session)
    public static int defaultPickBanLimit = 4;
    
    // Default max rounds (can be set before starting a session)
    public static int defaultMaxRounds = 2;

    private static boolean isInitialized = false;

    public static Map<UUID, Long> waitingForVersionPacketPlayersMap = new HashMap<>();

    public static void initializeServer() {
        lockout = null;
        compassHandler = null;
        gameStartRunnables.clear();
        waitingForVersionPacketPlayersMap.clear();

        // Ideally, rejoining a world gets detected here, and this data doesn't get wiped
        BIOME_LOCATE_DATA.clear();
        STRUCTURE_LOCATE_DATA.clear();
        AVAILABLE_DYE_COLORS.clear();

        LockoutConfig.load(); // reload config every time the server starts
        boardSize = LockoutConfig.getInstance().boardSize;
        Lockout.log("Using default board size: " + boardSize);

        if (isInitialized) return;
        isInitialized = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(new AllowChatMessageEventHandler());

        ServerPlayerEvents.AFTER_RESPAWN.register(new AfterRespawnEventHandler());

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(new AfterPlayerChangeWorldEventHandler());

        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinEventHandler());

        ServerTickEvents.END_SERVER_TICK.register(new EndServerTickEventHandler());

        // Add timeout handler for version packet checking
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            long currentTime = System.currentTimeMillis();
            long timeoutMs = 5000; // 5 second timeout
            
            // Check for players who haven't responded within timeout
            waitingForVersionPacketPlayersMap.entrySet().removeIf(entry -> {
                UUID playerUuid = entry.getKey();
                long joinTime = entry.getValue();
                
                if (currentTime - joinTime > timeoutMs) {
                    // Timeout expired, kick player
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                    if (player != null) {
                        player.networkHandler.disconnect(Text.of("Missing Lockout mod.\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
                    }
                    return true; // Remove from map
                }
                return false; // Keep in map
            });
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(new AfterDeathEventHandler());

        UseBlockCallback.EVENT.register(new UseBlockEventHandler());
        
        UseBlockCallback.EVENT.register(new CopperGolemConstructionHandler());

        ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedEventHandler());

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            isInitialized = false;
        });

        UseEntityCallback.EVENT.register(new HorseArmorEquipHandler());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, minecraftServer) -> {
            waitingForVersionPacketPlayersMap.remove(handler.getPlayer().getUuid());
        });

        ServerPlayNetworking.registerGlobalReceiver(LockoutVersionPayload.ID, (payload, context) -> {
            // Client has Lockout mod, compare versions, then kick or initialize
            ServerPlayerEntity player = context.player();
            waitingForVersionPacketPlayersMap.remove(player.getUuid());

            String version = payload.version();
            if (!version.equals(LockoutInitializer.MOD_VERSION.getFriendlyString())) {
                player.networkHandler.disconnect(Text.of("Wrong Lockout version: v" + version + ".\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
                return;
            }

            if (!Lockout.isLockoutRunning(lockout)) return;

            if (lockout.isLockoutPlayer(player.getUuid())) {
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), String.join("\n", hasTooltipInfo.getTooltip(team, player))));
                    }
                }
                player.changeGameMode(GameMode.SURVIVAL);
            } else {
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), String.join("\n", hasTooltipInfo.getSpectatorTooltip())));
                    }
                }
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You are spectating this match.").formatted(Formatting.GRAY, Formatting.ITALIC));
            }

            ServerPlayNetworking.send(player, lockout.getTeamsGoalsPacket());
            ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());
            if (lockout.hasStarted()) {
                ServerPlayNetworking.send(player, StartLockoutPayload.INSTANCE);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdatePicksBansPayload.ID, (payload, context) -> {
            // Store picks/bans and goal-to-player mapping on server-side
            SERVER_PICKS.clear();
            SERVER_PICKS.addAll(payload.picks());
            SERVER_BANS.clear();
            SERVER_BANS.addAll(payload.bans());
            SERVER_GOAL_TO_PLAYER_MAP.clear();
            SERVER_GOAL_TO_PLAYER_MAP.putAll(payload.goalToPlayerMap());
            
            // Broadcast picks/bans update to all other players
            for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList()) {
                if (otherPlayer != context.player()) {
                    ServerPlayNetworking.send(otherPlayer, payload);
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(BroadcastPickBanPayload.ID, (payload, context) -> {
            // Broadcast the pick/ban action message to all players
            server.execute(() -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SyncPickBanLimitPayload.ID, (payload, context) -> {
            // Broadcast the pick/ban limit to all players
            server.execute(() -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UploadBoardTypePayload.ID, (payload, context) -> {
            server.execute(() -> {
                ServerPlayerEntity player = context.player();
                
                // Check permissions - only operators level 2+ can upload board types
                if (!Permissions.check(player, PLACEHOLDER_PERM_STRING, 2)) {
                    player.sendMessage(Text.literal("You don't have permission to set board types!").formatted(Formatting.RED), false);
                    return;
                }
                
                // Store the uploaded board type data on the server
                boardType = payload.boardTypeName();
                boardTypeExcludedGoals = new java.util.ArrayList<>(payload.excludedGoals());
                
                // Clear all picks and bans (both server-side and goal groups)
                GoalGroup.PICKS.getGoals().clear();
                GoalGroup.BANS.getGoals().clear();
                GoalGroup.PENDING_PICKS.getGoals().clear();
                GoalGroup.PENDING_BANS.getGoals().clear();
                GoalGroup.clearGoalPlayers();
                SERVER_PICKS.clear();
                SERVER_BANS.clear();
                SERVER_GOAL_TO_PLAYER_MAP.clear();
                
                // Broadcast to all clients
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, new SetBoardTypePayload(boardType, boardTypeExcludedGoals));
                    // Also send picks/bans update to clear client-side lists
                    ServerPlayNetworking.send(p, new UpdatePicksBansPayload(SERVER_PICKS, SERVER_BANS, SERVER_GOAL_TO_PLAYER_MAP));
                }
                
                player.sendMessage(Text.literal("Board type '" + boardType + "' uploaded to server (" + boardTypeExcludedGoals.size() + " goals excluded)."), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AnnounceGoalFocusPayload.ID, (payload, context) -> {
            server.execute(() -> {
                ServerPlayerEntity player = context.player();
                
                // Check if lockout is active
                if (!Lockout.exists(lockout)) {
                    return;
                }
                
                // Get player's team
                Team playerTeam = player.getScoreboardTeam();
                if (playerTeam == null) {
                    return;
                }
                
                // Cooldown check
                UUID playerId = player.getUuid();
                long currentTime = System.currentTimeMillis();
                List<Long> timestamps = playerPingTimestamps.getOrDefault(playerId, new ArrayList<>());
                
                // Remove timestamps older than the cooldown window
                timestamps.removeIf(time -> currentTime - time > COOLDOWN_WINDOW_MS);
                
                // Check if player has exceeded the limit
                if (timestamps.size() >= MAX_PINGS) {
                    long oldestPing = timestamps.get(0);
                    long timeUntilCooldownEnds = COOLDOWN_WINDOW_MS - (currentTime - oldestPing);
                    int secondsRemaining = (int) Math.ceil(timeUntilCooldownEnds / 1000.0);
                    
                    player.sendMessage(
                        Text.literal("Your ping is on cooldown, please wait " + secondsRemaining + " seconds!")
                            .formatted(Formatting.RED),
                        false
                    );
                    return;
                }
                
                // Add current ping timestamp
                timestamps.add(currentTime);
                playerPingTimestamps.put(playerId, timestamps);
                
                // Format goal name
                String goalName = org.apache.commons.lang3.text.WordUtils.capitalize(
                    payload.goalId().replace("_", " ").toLowerCase(), ' '
                );
                
                // Create message based on action type
                Text message;
                float pitch;
                if (payload.isReminder()) {
                    message = Text.literal(player.getName().getString() + " is reminding their team about " + goalName + "!")
                        .formatted(Formatting.GOLD);
                    pitch = 0.5f;
                } else {
                    message = Text.literal(player.getName().getString() + " is currently working on " + goalName + "!")
                        .formatted(Formatting.AQUA);
                    pitch = 1.0f;
                }
                
                // Send message and play sound to all team members
                for (ServerPlayerEntity teamPlayer : server.getPlayerManager().getPlayerList()) {
                    Team team = teamPlayer.getScoreboardTeam();
                    if (team != null && team.getName().equals(playerTeam.getName())) {
                        teamPlayer.sendMessage(message, false);
                        teamPlayer.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 1.0f, pitch);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LockPickBanSelectionsPayload.ID, (payload, context) -> {
            server.execute(() -> {
                if (activePickBanSession == null) {
                    context.player().sendMessage(Text.literal("No active pick/ban session."), false);
                    return;
                }

                String playerName = context.player().getName().getString();
                
                // Verify player is on active team
                if (!activePickBanSession.isPlayerOnActiveTeam(playerName)) {
                    context.player().sendMessage(Text.literal("It's not your team's turn!"), false);
                    return;
                }
                
                // Get pending picks/bans from the payload (sent from client)
                List<String> pendingPicks = new ArrayList<>(payload.pendingPicks());
                List<String> pendingBans = new ArrayList<>(payload.pendingBans());
                Map<String, String> goalToPlayerMap = payload.goalToPlayerMap();
                
                // Debug: log the sizes
                System.out.println("DEBUG: Pending picks size: " + pendingPicks.size() + ", Pending bans size: " + pendingBans.size() + ", Limit: " + activePickBanSession.getSelectionLimit());
                System.out.println("DEBUG: Pending picks: " + pendingPicks);
                System.out.println("DEBUG: Pending bans: " + pendingBans);
                
                // Verify they have the right number of selections based on round type
                int limit = activePickBanSession.getSelectionLimit();
                if (activePickBanSession.isBanRound()) {
                    // Ban round: only check bans
                    if (pendingBans.size() != limit) {
                        context.player().sendMessage(
                            Text.literal("You must select exactly " + limit + " bans before locking."),
                            false
                        );
                        return;
                    }
                } else {
                    // Pick round: only check picks
                    if (pendingPicks.size() != limit) {
                        context.player().sendMessage(
                            Text.literal("You must select exactly " + limit + " picks before locking."),
                            false
                        );
                        return;
                    }
                }
                
                // Clear any existing pending selections and add the new ones
                activePickBanSession.clearPendingSelections();
                for (String goalId : pendingPicks) {
                    activePickBanSession.addPendingPick(goalId);
                    // Store the player who selected this goal
                    String teamPlayerName = goalToPlayerMap.get(goalId);
                    if (playerName != null) {
                        activePickBanSession.setGoalPlayer(goalId, playerName);
                    }
                }
                for (String goalId : pendingBans) {
                    activePickBanSession.addPendingBan(goalId);
                    // Store the player who selected this goal
                    String teamPlayerName = goalToPlayerMap.get(goalId);
                    if (teamPlayerName != null) {
                        activePickBanSession.setGoalPlayer(goalId, teamPlayerName);
                    }
                }
                
                // Lock the selections
                activePickBanSession.lockCurrentSelections();
                
                // Clear the pending goal groups
                me.marin.lockout.generator.GoalGroup.PENDING_PICKS.getGoals().clear();
                me.marin.lockout.generator.GoalGroup.PENDING_BANS.getGoals().clear();
                
                // Check if session is complete
                if (activePickBanSession.isComplete()) {
                    // Session complete - finalize picks/bans
                    SERVER_PICKS.clear();
                    SERVER_PICKS.addAll(activePickBanSession.getAllLockedPicks());
                    SERVER_BANS.clear();
                    SERVER_BANS.addAll(activePickBanSession.getAllLockedBans());
                    
                    // Update global pick/ban lists
                    me.marin.lockout.generator.GoalGroup.PICKS.getGoals().clear();
                    me.marin.lockout.generator.GoalGroup.PICKS.getGoals().addAll(SERVER_PICKS);
                    me.marin.lockout.generator.GoalGroup.BANS.getGoals().clear();
                    me.marin.lockout.generator.GoalGroup.BANS.getGoals().addAll(SERVER_BANS);
                    
                    // Get goal-to-player map from session
                    Map<String, String> finalGoalToPlayerMap = activePickBanSession.getGoalToPlayerMap();
                    
                    // Broadcast end to all players with final picks/bans
                    EndPickBanSessionPayload endPayload = new EndPickBanSessionPayload(
                        false,
                        new HashSet<>(SERVER_PICKS),
                        new HashSet<>(SERVER_BANS),
                        finalGoalToPlayerMap
                    );
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, endPayload);
                    }
                    
                    // Clear the session
                    activePickBanSession = null;
                    
                    server.getPlayerManager().broadcast(
                        Text.literal("Pick/ban session completed! Picks and bans have been finalized.").withColor(0xFFAA00),
                        false
                    );
                } else {
                    // Use the goal-to-player map from the session
                    Map<String, String> newGoalToPlayerMap = activePickBanSession.getGoalToPlayerMap();
                    
                    // Broadcast updated session state to all players
                    UpdatePickBanSessionPayload updatePayload = new UpdatePickBanSessionPayload(
                        activePickBanSession.getCurrentRound(),
                        activePickBanSession.isTeam1Turn(),
                        activePickBanSession.getTeam1Name(),
                        activePickBanSession.getTeam2Name(),
                        activePickBanSession.getAllLockedPicks(),
                        activePickBanSession.getAllLockedBans(),
                        activePickBanSession.getPendingPicks(),
                        activePickBanSession.getPendingBans(),
                        activePickBanSession.getSelectionLimit(),
                        newGoalToPlayerMap,
                        activePickBanSession.getMaxRounds()
                    );
                    
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, updatePayload);
                    }
                    
                    server.getPlayerManager().broadcast(
                        Text.literal("Round " + activePickBanSession.getCurrentRound() + "/" + activePickBanSession.getMaxRounds() + " - " + 
                                   activePickBanSession.getCurrentActiveTeamName() + "'s turn").withColor(0xFFAA00),
                        false
                    );
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CustomBoardPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            // Permission checks handled elsewhere; accept for now to allow client-side custom board packets.

            boolean clearBoard = payload.boardOrClear().isEmpty();
            if (clearBoard) {
                CUSTOM_BOARD = null;
                player.sendMessage(Text.literal("Removed custom board."));
            } else {
                // validate board
                List<String> invalidGoals = new ArrayList<>();
                for (Pair<String, String> goal : payload.boardOrClear().get()) {
                    if (!GoalRegistry.INSTANCE.isGoalValid(goal.getA(), goal.getB())) {
                        invalidGoals.add(" - '" + goal.getA() + "'" + ("null".equals(goal.getB()) ? "" : (" with data: '" + goal.getB() + "'")));
                    }
                }
                if (!invalidGoals.isEmpty()) {
                    player.sendMessage(Text.literal("Invalid board. Could not create goals:\n" + String.join("\n", invalidGoals)));
                    return;
                }
                CUSTOM_BOARD = new LockoutBoard(payload.boardOrClear().get());
                player.sendMessage(Text.literal("Set custom board."));
            }
        });
    }

    public static int startPickBanSession(CommandContext<ServerCommandSource> context, Team team1, Team team2) {
        if (activePickBanSession != null) {
            context.getSource().sendError(Text.literal("A pick/ban session is already active! Use /CancelPickBanSession to cancel it first."));
            return 0;
        }

        // Clear all goal groups before starting the session
        GoalGroup.PICKS.getGoals().clear();
        GoalGroup.BANS.getGoals().clear();
        GoalGroup.PENDING_PICKS.getGoals().clear();
        GoalGroup.PENDING_BANS.getGoals().clear();

        // Create new session with the default limit and max rounds
        activePickBanSession = new PickBanSession(team1, team2, server, defaultMaxRounds);
        activePickBanSession.setSelectionLimit(defaultPickBanLimit);

        // Broadcast session start to all players
        StartPickBanSessionPayload payload = new StartPickBanSessionPayload(
            team1.getName(),
            team2.getName(),
            activePickBanSession.getSelectionLimit()
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }

        context.getSource().sendMessage(
            Text.literal("Started pick/ban session: " + team1.getName() + " vs " + team2.getName())
        );
        
        server.getPlayerManager().broadcast(
            Text.literal("Pick/ban session started! Round 1/" + activePickBanSession.getMaxRounds() + " - " + team1.getName() + "'s turn").withColor(0x55FF55),
            false
        );

        return 1;
    }

    public static LocateData locateBiome(MinecraftServer server, RegistryKey<Biome> biome) {
        if (BIOME_LOCATE_DATA.containsKey(biome)) return BIOME_LOCATE_DATA.get(biome);

        var spawnPoint = server.getOverworld().getSpawnPoint();
        var currentPos = spawnPoint.getPos();

        var pair = server.getOverworld().locateBiome(
                biomeRegistryEntry -> biomeRegistryEntry.matchesKey(biome),
                currentPos,
                LOCATE_SEARCH,
                32,
                64);

        LocateData data= new LocateData(false,0);
        if (pair != null) {
            int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        BIOME_LOCATE_DATA.put(biome, data);

        return data;
    }

    public static LocateData locateStructure(MinecraftServer server, RegistryKey<Structure> structure) {
        if (STRUCTURE_LOCATE_DATA.containsKey(structure)) return STRUCTURE_LOCATE_DATA.get(structure);

        var spawnPoint = server.getOverworld().getSpawnPoint();
        var currentPos = spawnPoint.getPos();

        Registry<Structure> registry = server.getOverworld().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        RegistryEntryList<Structure> structureList = RegistryEntryList.of(registry.getOrThrow(structure));

        var pair = server.getOverworld().getChunkManager().getChunkGenerator().locateStructure(
                server.getOverworld(),
                structureList,
                currentPos,
                LOCATE_SEARCH,
                false);

        LocateData data = new LocateData(false, 0);
        if (pair != null) {
            int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        STRUCTURE_LOCATE_DATA.put(structure, data);

        return data;
    }

    public static int lockoutCommandLogic(CommandContext<ServerCommandSource> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, false);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    public static int blackoutCommandLogic(CommandContext<ServerCommandSource> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, true);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    public static int lockoutRandomCommandLogic(CommandContext<ServerCommandSource> context) {
        Integer teamCount = null;
        try {
            teamCount = context.getArgument("team count", Integer.class);
        } catch (Exception ignored) {}
        
        List<LockoutTeamServer> teams = createRandomTeams(context, teamCount);
        if (teams == null) return 0;

        startLockout(teams);
        return 1;
    }

    private static List<LockoutTeamServer> createRandomTeams(CommandContext<ServerCommandSource> context, Integer teamCount) {
        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> allPlayers = new ArrayList<>(playerManager.getPlayerList());
        
        if (allPlayers.isEmpty()) {
            context.getSource().sendError(Text.literal("No players online to create teams."));
            return null;
        }

        if (allPlayers.size() < 2) {
            context.getSource().sendError(Text.literal("Need at least 2 players online for random lockout."));
            return null;
        }

        // Shuffle players
        java.util.Collections.shuffle(allPlayers);
        
        List<LockoutTeamServer> teams = new ArrayList<>();
        
        if (teamCount == null) {
            // Default: Create 1v1 teams (each player on their own team)
            for (int i = 0; i < allPlayers.size(); i++) {
                ServerPlayerEntity player = allPlayers.get(i);
                teams.add(new LockoutTeamServer(
                    List.of(player.getName().getString()),
                    Formatting.byColorIndex(Lockout.COLOR_ORDERS[i % Lockout.COLOR_ORDERS.length]),
                    server
                ));
            }
        } else {
            // Create specified number of teams
            if (teamCount > allPlayers.size()) {
                context.getSource().sendError(Text.literal("Cannot create " + teamCount + " teams with only " + allPlayers.size() + " players."));
                return null;
            }
            
            // Create lists to hold player names for each team
            List<List<String>> teamPlayerNames = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) {
                teamPlayerNames.add(new ArrayList<>());
            }
            
            // Assign players to teams in round-robin fashion
            for (int i = 0; i < allPlayers.size(); i++) {
                ServerPlayerEntity player = allPlayers.get(i);
                int teamIndex = i % teamCount;
                teamPlayerNames.get(teamIndex).add(player.getName().getString());
            }
            
            // Create teams with the player lists
            for (int i = 0; i < teamCount; i++) {
                teams.add(new LockoutTeamServer(
                    teamPlayerNames.get(i),
                    Formatting.byColorIndex(Lockout.COLOR_ORDERS[i % Lockout.COLOR_ORDERS.length]),
                    server
                ));
            }
        }
        
        return teams;
    }

    private static void startLockout(List<LockoutTeamServer> teams) {
        // Clear old runnables
        gameStartRunnables.clear();

        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> allServerPlayers = playerManager.getPlayerList();
        List<UUID> allLockoutPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .toList();
        List<UUID> allSpectatorPlayers = allServerPlayers.stream()
                .map(ServerPlayerEntity::getUuid)
                .filter(uuid -> !allLockoutPlayers.contains(uuid))
                .toList();

        for (ServerPlayerEntity serverPlayer : allServerPlayers) {
            serverPlayer.getInventory().clear();
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
            serverPlayer.clearStatusEffects();
            serverPlayer.getHungerManager().setSaturationLevel(5);
            serverPlayer.getHungerManager().setFoodLevel(20);
            serverPlayer.getHungerManager().exhaustion = 0.0f;
            serverPlayer.setExperienceLevel(0);
            serverPlayer.setExperiencePoints(0);
            serverPlayer.setOnFire(false);

            // Clear all stats
            for (@SuppressWarnings("unchecked") StatType<Object> statType : new StatType[]{Stats.CRAFTED, Stats.MINED, Stats.USED, Stats.BROKEN, Stats.PICKED_UP, Stats.DROPPED, Stats.KILLED, Stats.KILLED_BY, Stats.CUSTOM}) {
                for (Identifier id : statType.getRegistry().getIds()) {
                    serverPlayer.resetStat(statType.getOrCreateStat(statType.getRegistry().get(id)));
                }
            }
            serverPlayer.getStatHandler().sendStats(serverPlayer);
            // Clear all advancements
            AdvancementCommand.Operation.REVOKE.processAll(serverPlayer, server.getAdvancementLoader().getAdvancements(), false);

            if (allLockoutPlayers.contains(serverPlayer.getUuid())) {
                serverPlayer.changeGameMode(GameMode.ADVENTURE);
            } else {
                serverPlayer.changeGameMode(GameMode.SPECTATOR);
                serverPlayer.sendMessage(Text.literal("You are spectating this match.").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }

        ServerWorld world = server.getWorld(ServerWorld.OVERWORLD);

        // Generate & set board
        LockoutBoard lockoutBoard;
        if (CUSTOM_BOARD == null) {
            // Populate GoalGroup with server-side picks/bans before generation
            me.marin.lockout.generator.GoalGroup.PICKS.getGoals().clear();
            me.marin.lockout.generator.GoalGroup.PICKS.getGoals().addAll(SERVER_PICKS);
            me.marin.lockout.generator.GoalGroup.BANS.getGoals().clear();
            me.marin.lockout.generator.GoalGroup.BANS.getGoals().addAll(SERVER_BANS);
            
            BoardGenerator boardGenerator = new BoardGenerator(GoalRegistry.INSTANCE.getRegisteredGoals(), teams, AVAILABLE_DYE_COLORS, BIOME_LOCATE_DATA, STRUCTURE_LOCATE_DATA);
            lockoutBoard = boardGenerator.generateBoard(boardSize, boardType, boardTypeExcludedGoals);

            // Check if board generation failed due to insufficient goals
            if (lockoutBoard == null) {
                String errorMessage = "Cannot generate board: Not enough goals enabled in goal-pool.yml. Please enable more goals or reduce board size.";
                for (UUID playerUuid : allLockoutPlayers) {
                    ServerPlayerEntity player = playerManager.getPlayer(playerUuid);
                    if (player != null) {
                        player.sendMessage(Text.literal(errorMessage).formatted(Formatting.RED), false);
                    }
                }
                return; // Abort lockout start
            }
            
            // Clear after generation
            me.marin.lockout.generator.GoalGroup.PICKS.getGoals().clear();
            me.marin.lockout.generator.GoalGroup.BANS.getGoals().clear();
        } else {
            // Reset custom board (TODO: do this somewhere else)
            for (Goal goal : CUSTOM_BOARD.getGoals()) {
                goal.setCompleted(false, null);
            }
            lockoutBoard = CUSTOM_BOARD;
        }

        lockout = new Lockout(lockoutBoard, teams);
        lockout.setTicks(-20L * lockoutStartTime); // see Lockout#ticks

        compassHandler = new CompassItemHandler(allLockoutPlayers, playerManager);

        List<Goal> tooltipGoals = new ArrayList<>(lockout.getBoard().getGoals()).stream().filter(g -> g instanceof HasTooltipInfo).toList();
        for (Goal goal : tooltipGoals) {
            // Update teams tooltip
            for (LockoutTeam team : lockout.getTeams()) {
                ((LockoutTeamServer) team).sendTooltipUpdate((Goal & HasTooltipInfo) goal, false);
            }
            // Update spectator tooltip
            if (!allSpectatorPlayers.isEmpty()) {
                var payload = new UpdateTooltipPayload(goal.getId(), String.join("\n", ((HasTooltipInfo) goal).getSpectatorTooltip()));
                for (UUID spectator : allSpectatorPlayers) {
                    ServerPlayNetworking.send(playerManager.getPlayer(spectator), payload);
                }
            }
        }

        for (ServerPlayerEntity player : allServerPlayers) {
            ServerPlayNetworking.send(player, lockout.getTeamsGoalsPacket());
            ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());

            if (!lockout.isSoloBlackout() && lockout.isLockoutPlayer(player.getUuid())) {
                player.giveItemStack(compassHandler.newCompass());
            }
        }

        world.setTimeOfDay(0);
        
        var unfreezeCommand = "tick unfreeze";
        var unfreezeParseResults = server.getCommandManager().getDispatcher().parse(unfreezeCommand, server.getCommandSource());
        server.getCommandManager().execute(unfreezeParseResults, unfreezeCommand);

        for (int i = 3; i >= 0; i--) {
            if (i > 0) {
                final int secs = i;
                ((LockoutRunnable) () -> {
                    playerManager.broadcast(Text.literal("Starting in " + secs + "..."), false);
                }).runTaskAfter(20L * (lockoutStartTime - i));
            } else {
                ((LockoutRunnable) () -> {
                    lockout.setStarted(true);

                    for (ServerPlayerEntity player : allServerPlayers) {
                        if (player == null) continue;
                        ServerPlayNetworking.send(player, StartLockoutPayload.INSTANCE);
                        if (allLockoutPlayers.contains(player.getUuid())) {
                            player.changeGameMode(GameMode.SURVIVAL);
                        }
                    }
                    server.getPlayerManager().broadcast(Text.literal(lockout.getModeName() + " has begun."), false);
                }).runTaskAfter(20L * lockoutStartTime);
            }
        }
    }

    private static int parseArgumentsIntoTeams(List<LockoutTeamServer> teams, CommandContext<ServerCommandSource> context, boolean isBlackout) {
        String argument = null;

        PlayerManager playerManager = server.getPlayerManager();

        try {
            argument = context.getArgument("player names", String.class);
            String[] playerNames = argument.split(" +");
            
            if (isBlackout) {
                if (playerNames.length == 0) {
                    context.getSource().sendError(Text.literal("Not enough players listed."));
                    return 0;
                }

                List<String> validPlayerNames = new ArrayList<>();
                for (String playerName : playerNames) {
                    ServerPlayerEntity player = playerManager.getPlayer(playerName);
                    if (player == null) {
                        context.getSource().sendError(Text.literal("Player " + playerName + " is not online."));
                        return 0;
                    }
                    validPlayerNames.add(player.getName().getString());
                }
                teams.add(new LockoutTeamServer(validPlayerNames, Formatting.byColorIndex(Lockout.COLOR_ORDERS[0]), server));
                return 1;

            } else {
                if (playerNames.length < 2) {
                    context.getSource().sendError(Text.literal("Not enough players listed. You need at least 2 players."));
                    return 0;
                }
                if (playerNames.length > 16) {
                    context.getSource().sendError(Text.literal("Too many players listed."));
                    return 0;
                }

                for (int i = 0; i < playerNames.length; i++) {
                    String playerName = playerNames[i];
                    ServerPlayerEntity player = playerManager.getPlayer(playerName);
                    if (player == null) {
                        context.getSource().sendError(Text.literal("Player " + playerName + " is not online."));
                        return 0;
                    }
                    teams.add(new LockoutTeamServer(List.of(player.getName().getString()), Formatting.byColorIndex(Lockout.COLOR_ORDERS[i]), server));
                }
                return 1;
            }

        } catch (Exception ignored) {}

        if (argument == null) {
            try {
                ServerScoreboard scoreboard = server.getScoreboard();

                argument = context.getArgument(isBlackout ? "team name" : "team names", String.class);
                String[] teamNames = argument.split(" +");
                if (isBlackout) {
                    if (teamNames.length == 0) {
                        context.getSource().sendError(Text.literal("Not enough teams listed."));
                        return 0;
                    }
                    if (teamNames.length > 1) {
                        context.getSource().sendError(Text.literal("Only one team can play Blackout."));
                        return 0;
                    }
                } else {
                    if (teamNames.length < 2) {
                        context.getSource().sendError(Text.literal("Not enough teams listed. Make sure you separate team names with spaces."));
                        return 0;
                    }
                    if (teamNames.length > 16) {
                        context.getSource().sendError(Text.literal("Too many teams listed."));
                        return 0;
                    }
                }

                List<Team> scoreboardTeams = new ArrayList<>();
                for (String teamName : teamNames) {
                    Team team = scoreboard.getTeam(teamName);
                    if (team == null) {
                        context.getSource().sendError(Text.literal("Team " + teamName + " is invalid."));
                        return 0;
                    }
                    for (String player : team.getPlayerList()) {
                        if (playerManager.getPlayer(player) == null) {
                            context.getSource().sendError(Text.literal("Player " + player + " on team " + teamName + " is invalid. Remove them from the team and try again."));
                            return 0;
                        }
                    }
                    scoreboardTeams.add(team);
                }
                for (Team team : scoreboardTeams) {
                    if (team.getPlayerList().isEmpty()) {
                        context.getSource().sendError(Text.literal("Team " + team.getName() + " doesn't have any players."));
                        return 0;
                    }
                    Formatting teamColor = team.getColor();
                    if (teamColor.getColorValue() == null || teamHasColor(teams, teamColor)) {
                        // Select an available color.
                        boolean found = false;
                        for (int colorOrder : Lockout.COLOR_ORDERS) {
                            if (!teamHasColor(teams, Formatting.byColorIndex(colorOrder))) {
                                found = true;
                                team.setColor(Formatting.byColorIndex(colorOrder));
                                break;
                            }
                        }
                        if (!found) {
                            context.getSource().sendError(Text.literal("Could not find assignable color for team " + team.getName() + ". Try recreating teams."));
                            return 0;
                        }
                    }
                    List<String> actualPlayerNames = new ArrayList<>();
                    for (String playerName : team.getPlayerList()) {
                        actualPlayerNames.add(playerManager.getPlayer(playerName).getName().getString());
                    }
                    teams.add(new LockoutTeamServer(new ArrayList<>(actualPlayerNames), team.getColor(), server));
                }
            } catch (Exception ignored) {}
        }

        if (argument == null) {
            context.getSource().sendError(Text.literal("Illegal argument."));
            return 0;
        }
        return 1;
    }

    private static boolean teamHasColor(List<LockoutTeamServer> teams, Formatting color) {
        for (LockoutTeam lockoutTeam : teams) {
            if (lockoutTeam.getColor() == color) {
                return true;
            }
        }
        return false;
    }

    public static int setChat(CommandContext<ServerCommandSource> context, ChatManager.Type type) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This is a player-only command."));
            return 0;
        }

        ChatManager.Type curr = ChatManager.getChat(player);
        if (curr == type) {
            player.sendMessage(Text.of("You are already chatting in " + type.name() + "."));
        } else {
            player.sendMessage(Text.of("You are now chatting in " + type.name() + "."));
            ChatManager.setChat(player, type);
        }
        return 1;
    }

    public static int giveGoal(CommandContext<ServerCommandSource> context) {
        try {
            if (!Lockout.isLockoutRunning(lockout)) {
                context.getSource().sendError(Text.literal("There's no active lockout match."));
                return 0;
            }

            int idx = context.getArgument("goal number", Integer.class);

            Collection<PlayerConfigEntry> playerConfigs;
            try {
                playerConfigs = GameProfileArgumentType.getProfileArgument(context, "player name");
            } catch (CommandSyntaxException e) {
                context.getSource().sendError(Text.literal("Invalid target."));
                return 0;
            }

            if (playerConfigs.size() != 1) {
                context.getSource().sendError(Text.literal("Invalid number of targets."));
                return 0;
            }
            PlayerConfigEntry playerConfig = playerConfigs.stream().findFirst().get();
            if (!lockout.isLockoutPlayer(playerConfig.id())) {
                context.getSource().sendError(Text.literal("Player " + playerConfig.name() + " is not playing Lockout."));
                return 0;
            }

            if (idx > lockout.getBoard().getGoals().size()) {
                context.getSource().sendError(Text.literal("Goal number does not exist on the board."));
                return 0;
            }
            Goal goal = lockout.getBoard().getGoals().get(idx - 1);

            context.getSource().sendMessage(Text.of("Gave " + playerConfig.name() + " goal \"" + goal.getGoalName() + "\"."));
            lockout.updateGoalCompletion(goal, playerConfig.id());
            return 1;
        } catch (RuntimeException e) {
            Lockout.error(e);
            return 0;
        }
    }

    public static int setStartTime(CommandContext<ServerCommandSource> context) {
        int seconds = context.getArgument("seconds", Integer.class);

        lockoutStartTime = seconds;
        context.getSource().sendMessage(Text.of("Updated start time to " + seconds + "s."));
        return 1;
    }

    public static int setBoardSize(CommandContext<ServerCommandSource> context) {
        int size = context.getArgument("board size", Integer.class);

        boardSize = size;
        context.getSource().sendMessage(Text.of("Updated board size to " + size + "."));
        return 1;
    }

}