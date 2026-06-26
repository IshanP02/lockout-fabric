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
import me.marin.lockout.network.GoalDetailsPayload;
import me.marin.lockout.network.LockPickBanSelectionsPayload;
import me.marin.lockout.network.LockoutVersionPayload;
import me.marin.lockout.network.RequestGoalDetailsPayload;
import me.marin.lockout.network.SetBoardTypePayload;
import me.marin.lockout.network.StartLockoutPayload;
import me.marin.lockout.network.StartPickBanSessionPayload;
import me.marin.lockout.network.SyncPickBanLimitPayload;
import me.marin.lockout.network.UpdatePickBanSessionPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import me.marin.lockout.network.UpdateTooltipPayload;
import me.marin.lockout.network.UploadBoardTypePayload;
import me.marin.lockout.server.handlers.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderSet;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import oshi.util.tuples.Pair;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.marin.lockout.Constants.PLACEHOLDER_PERM_STRING;

public class LockoutServer {

    public static final int LOCATE_SEARCH = 750;
    public static final Map<ResourceKey<Biome>, LocateData> BIOME_LOCATE_DATA = new HashMap<>();
    public static final Map<ResourceKey<Structure>, LocateData> STRUCTURE_LOCATE_DATA = new HashMap<>();
    public static final List<DyeColor> AVAILABLE_DYE_COLORS = new ArrayList<>();

    private static int lockoutStartTime = 60;
    private static int gracePeriodSeconds = 0;
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

    public static Map<UUID, Long> waitingForVersionPacketPlayersMap = new ConcurrentHashMap<>();
    
    // Track player death times for respawn grace period (30 seconds)
    public static Map<UUID, Long> playerDeathTimes = new ConcurrentHashMap<>();

    public static void initializeServer() {
        lockout = null;
        compassHandler = null;
        gameStartRunnables.clear();
        waitingForVersionPacketPlayersMap.clear();
        playerDeathTimes.clear();

        // Ideally, rejoining a world gets detected here, and this data doesn't get wiped
        BIOME_LOCATE_DATA.clear();
        STRUCTURE_LOCATE_DATA.clear();
        AVAILABLE_DYE_COLORS.clear();

        LockoutConfig.load(); // reload config every time the server starts
        boardSize = LockoutConfig.getInstance().boardSize;
        Lockout.log("Using default board size: " + boardSize);
    }

    /**
     * Registers all server-side event handlers once when the mod initializes.
     * This method is called from LockoutInitializer to ensure handlers are only registered once,
     * not every time a world is loaded (which was causing the timer to multiply in blackout mode).
     */
    public static void registerServerEventHandlers() {
        if (isInitialized) return;
        isInitialized = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(new AllowChatMessageEventHandler());

        ServerPlayerEvents.AFTER_RESPAWN.register(new AfterRespawnEventHandler());

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(new AfterPlayerChangeWorldEventHandler());

        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinEventHandler());

        ServerTickEvents.END_SERVER_TICK.register(new EndServerTickEventHandler());

        // Add timeout handler for version packet checking
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            long currentTime = System.currentTimeMillis();
            long timeoutMs = 20000; // 20 second timeout
            
            // Check for players who haven't responded within timeout
            waitingForVersionPacketPlayersMap.entrySet().removeIf(entry -> {
                UUID playerUuid = entry.getKey();
                long joinTime = entry.getValue();
                
                if (currentTime - joinTime > timeoutMs) {
                    // Timeout expired, kick player
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                    if (player != null) {
                        player.connection.disconnect(Component.literal("Missing Lockout mod.\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
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
            waitingForVersionPacketPlayersMap.remove(handler.getPlayer().getUUID());
        });

        ServerPlayNetworking.registerGlobalReceiver(LockoutVersionPayload.ID, (payload, context) -> {
            // Client has Lockout mod, compare versions, then kick or initialize
            ServerPlayer player = context.player();
            waitingForVersionPacketPlayersMap.remove(player.getUUID());

            String version = payload.version();
            if (!version.equals(LockoutInitializer.MOD_VERSION.getFriendlyString())) {
                player.connection.disconnect(Component.literal("Wrong Lockout version: v" + version + ".\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
                return;
            }

            if (!Lockout.isLockoutRunning(lockout)) return;

            if (lockout.isLockoutPlayer(player.getUUID())) {
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        List<String> tooltip = hasTooltipInfo.getTooltip(team, player);
                        if (tooltip != null && !tooltip.isEmpty()) {
                            String tooltipText = String.join("\n", tooltip);
                            ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), tooltipText));
                        }
                    }
                }
                player.gameMode.setGameModeForPlayer(GameType.SURVIVAL, null);
            } else {
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof HasTooltipInfo hasTooltipInfo) {
                        List<String> spectatorTooltip = hasTooltipInfo.getSpectatorTooltip();
                        if (spectatorTooltip != null && !spectatorTooltip.isEmpty()) {
                            String tooltipText = String.join("\n", spectatorTooltip);
                            ServerPlayNetworking.send(player, new UpdateTooltipPayload(goal.getId(), tooltipText));
                        }
                    }
                }
                player.gameMode.setGameModeForPlayer(GameType.SPECTATOR, null);
                player.sendSystemMessage(Component.literal("You are spectating this match.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
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
            for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                if (otherPlayer != context.player()) {
                    ServerPlayNetworking.send(otherPlayer, payload);
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(BroadcastPickBanPayload.ID, (payload, context) -> {
            // Broadcast the pick/ban action message to all players
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SyncPickBanLimitPayload.ID, (payload, context) -> {
            // Broadcast the pick/ban limit to all players
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, payload);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UploadBoardTypePayload.ID, (payload, context) -> {
            server.execute(() -> {
                ServerPlayer player = context.player();
                
                // Check permissions - only operators level 2+ can upload board types
                if (!server.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()))) {
                    player.sendSystemMessage(Component.literal("You don't have permission to set board types!").withStyle(ChatFormatting.RED));
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
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(p, new SetBoardTypePayload(boardType, boardTypeExcludedGoals));
                    // Also send picks/bans update to clear client-side lists
                    ServerPlayNetworking.send(p, new UpdatePicksBansPayload(SERVER_PICKS, SERVER_BANS, SERVER_GOAL_TO_PLAYER_MAP));
                }
                
                player.sendSystemMessage(Component.literal("Board type '" + boardType + "' uploaded to server (" + boardTypeExcludedGoals.size() + " goals excluded)."));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AnnounceGoalFocusPayload.ID, (payload, context) -> {
            server.execute(() -> {
                ServerPlayer player = context.player();
                
                // Check if lockout is active
                if (!Lockout.exists(lockout)) {
                    return;
                }
                
                // Get player's team
                Team playerTeam = player.getTeam();
                if (playerTeam == null) {
                    return;
                }
                
                // Cooldown check
                UUID playerId = player.getUUID();
                long currentTime = System.currentTimeMillis();
                List<Long> timestamps = playerPingTimestamps.getOrDefault(playerId, new ArrayList<>());
                
                // Remove timestamps older than the cooldown window
                timestamps.removeIf(time -> currentTime - time > COOLDOWN_WINDOW_MS);
                
                // Check if player has exceeded the limit
                if (timestamps.size() >= MAX_PINGS) {
                    long oldestPing = timestamps.get(0);
                    long timeUntilCooldownEnds = COOLDOWN_WINDOW_MS - (currentTime - oldestPing);
                    int secondsRemaining = (int) Math.ceil(timeUntilCooldownEnds / 1000.0);
                    
                    player.sendSystemMessage(
                        Component.literal("Your ping is on cooldown, please wait " + secondsRemaining + " seconds!")
                            .withStyle(ChatFormatting.RED)
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
                Component message;
                float pitch;
                if (payload.isReminder()) {
                    message = Component.literal(player.getName().getString() + " is reminding their team about " + goalName + "!")
                        .withStyle(ChatFormatting.GOLD);
                    pitch = 0.5f;
                } else {
                    message = Component.literal(player.getName().getString() + " is currently working on " + goalName + "!")
                        .withStyle(ChatFormatting.AQUA);
                    pitch = 1.0f;
                }
                
                // Send message and play sound to all team members
                for (ServerPlayer teamPlayer : server.getPlayerList().getPlayers()) {
                    Team team = teamPlayer.getTeam();
                    if (team != null && team.getName().equals(playerTeam.getName())) {
                        teamPlayer.sendSystemMessage(message);
                        teamPlayer.connection.send(
                            new ClientboundSoundPacket(
                                SoundEvents.NOTE_BLOCK_HARP,
                                SoundSource.MASTER,
                                teamPlayer.getX(),
                                teamPlayer.getY(),
                                teamPlayer.getZ(),
                                1.0f,
                                pitch,
                                teamPlayer.getRandom().nextLong()
                            )
                        );
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestGoalDetailsPayload.ID, (payload, context) -> {
            server.execute(() -> {
                ServerPlayer spectator = context.player();
                
                // Check if lockout is active
                if (!Lockout.exists(lockout)) {
                    return;
                }
                
                // Find the goal
                Goal goal = lockout.getBoard().getGoals().stream()
                    .filter(g -> g !=null && g.getId().equals(payload.goalId()))
                    .findFirst()
                    .orElse(null);
                
                if (goal == null) {
                    return;
                }
                
                // Build detailed information
                StringBuilder details = new StringBuilder();
                
                // Check if it's a tooltip goal
                if (goal instanceof me.marin.lockout.lockout.interfaces.KillUniqueHostileMobsGoal killGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var hostiles = lockout.killedHostileTypes.getOrDefault(team, new java.util.LinkedHashSet<>());
                        String mobsList = hostiles.stream()
                            .map(type -> type.getDescription().getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(mobsList.isEmpty() ? "None" : mobsList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.BreedUniqueAnimalsGoal breedGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var animals = lockout.bredAnimalTypes.getOrDefault(team, new java.util.LinkedHashSet<>());
                        String animalsList = animals.stream()
                            .map(type -> type.getDescription().getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(animalsList.isEmpty() ? "None" : animalsList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.EatUniqueFoodsGoal eatGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        java.util.LinkedHashSet<net.minecraft.world.item.Item> foods = lockout.foodTypesEaten.getOrDefault(team, new java.util.LinkedHashSet<>());
                        String foodsList = foods.stream()
                            .map(item -> net.minecraft.network.chat.Component.translatable(item.getDescriptionId()).getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(foodsList.isEmpty() ? "None" : foodsList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.VisitUniqueBiomesGoal visitGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var biomes = lockout.biomesVisited.getOrDefault(team, new java.util.HashSet<>());
                        String biomesList = biomes.stream()
                            .map(id -> net.minecraft.network.chat.Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(biomesList.isEmpty() ? "None" : biomesList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.VisitAllSpecificBiomesGoal specificBiomesGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var visitedBiomes = specificBiomesGoal.getTrackerMap().getOrDefault(team, new java.util.LinkedHashSet<>());
                        String biomesList = visitedBiomes.stream()
                            .map(id -> net.minecraft.network.chat.Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(biomesList.isEmpty() ? "None" : biomesList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.LookAtUniqueMobsGoal lookGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var mobs = lockout.lookedAtMobTypes.getOrDefault(team, new java.util.LinkedHashSet<>());
                        String mobsList = mobs.stream()
                            .map(type -> type.getDescription().getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(mobsList.isEmpty() ? "None" : mobsList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.GetUniqueAdvancementsGoal advancementGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        var advancements = advancementGoal.getTrackerMap().getOrDefault(team, new java.util.LinkedHashSet<>());
                        String advancementsList = advancements.stream()
                            .map(id -> server.getAdvancements().get(id).value().display().get().getTitle().getString())
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(advancementsList.isEmpty() ? "None" : advancementsList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal damageGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        java.util.LinkedHashSet<net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>> damageSources = damageGoal.getTrackerMap().getOrDefault(team, new java.util.LinkedHashSet<>());
                        String sourcesList = damageSources.stream()
                            .map(key -> org.apache.commons.lang3.text.WordUtils.capitalize(key.identifier().getPath().replace("_", " "), ' '))
                            .collect(java.util.stream.Collectors.joining(", "));
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ")
                            .append(net.minecraft.ChatFormatting.GRAY).append(sourcesList.isEmpty() ? "None" : sourcesList)
                            .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.interfaces.ReachXPLevelGoal xpGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        // Find players on this team and their XP levels
                        var teamPlayers = team.getPlayerNames().stream()
                            .map(uuid -> server.getPlayerList().getPlayer(uuid))
                            .filter(p -> p != null)
                            .collect(java.util.stream.Collectors.toList());
                        
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(": ");
                        if (teamPlayers.isEmpty()) {
                            details.append(net.minecraft.ChatFormatting.GRAY).append("No players").append(net.minecraft.ChatFormatting.RESET);
                        } else {
                            String playerLevels = teamPlayers.stream()
                                .map(p -> p.getName().getString() + " (Lv " + p.experienceLevel + ")")
                                .collect(java.util.stream.Collectors.joining(", "));
                            details.append(net.minecraft.ChatFormatting.GRAY).append(playerLevels).append(net.minecraft.ChatFormatting.RESET);
                        }
                        details.append("\\n");
                    }
                } else if (goal instanceof me.marin.lockout.lockout.goals.advancement.GetHotTouristDestinationsAdvancementGoal hotTouristGoal) {
                    details.append("Goal: ").append(goal.getGoalName()).append("\\n");
                    for (LockoutTeam team : lockout.getTeams()) {
                        // Find players on this team and their nether biome progress
                        var teamPlayers = team.getPlayerNames().stream()
                            .map(uuid -> server.getPlayerList().getPlayer(uuid))
                            .filter(p -> p != null)
                            .collect(java.util.stream.Collectors.toList());
                        
                        net.minecraft.ChatFormatting teamColor = team.getColor() != null ? team.getColor() : net.minecraft.ChatFormatting.WHITE;
                        details.append(teamColor).append(team.getDisplayName()).append(net.minecraft.ChatFormatting.RESET).append(":\\n");
                        
                        if (teamPlayers.isEmpty()) {
                            details.append(net.minecraft.ChatFormatting.GRAY).append("  No players").append(net.minecraft.ChatFormatting.RESET).append("\\n");
                        } else {
                            // Get the advancement
                            net.minecraft.advancements.AdvancementHolder advancementEntry = server.getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "nether/explore_nether"));

                            for (net.minecraft.server.level.ServerPlayer player : teamPlayers) {
                                var netherBiomes = new java.util.LinkedHashSet<net.minecraft.resources.Identifier>();

                                if (advancementEntry != null) {
                                    net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancementEntry);
                                    for (String criterion : progress.getCompletedCriteria()) {
                                        netherBiomes.add(net.minecraft.resources.Identifier.parse(criterion));
                                    }
                                }
                                
                                String biomesList = netherBiomes.stream()
                                    .map(id -> net.minecraft.network.chat.Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString())
                                    .collect(java.util.stream.Collectors.joining(", "));
                                
                                details.append("  ").append(player.getName().getString()).append(": ")
                                    .append(net.minecraft.ChatFormatting.GRAY).append(netherBiomes.size()).append("/5 - ")
                                    .append(biomesList.isEmpty() ? "None" : biomesList)
                                    .append(net.minecraft.ChatFormatting.RESET).append("\\n");
                            }
                        }
                    }
                } 
                
                // Send details back to spectator only if there's content
                if (details.length() > 0) {
                    ServerPlayNetworking.send(spectator, new GoalDetailsPayload(payload.goalId(), details.toString()));
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(LockPickBanSelectionsPayload.ID, (payload, context) -> {
            server.execute(() -> {
                if (activePickBanSession == null) {
                    context.player().sendSystemMessage(Component.literal("No active pick/ban session."));
                    return;
                }

                String playerName = context.player().getName().getString();
                
                // Verify player is on active team
                if (!activePickBanSession.isPlayerOnActiveTeam(playerName)) {
                    context.player().sendSystemMessage(Component.literal("It's not your team's turn!"));
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
                        context.player().sendSystemMessage(
                            Component.literal("You must select exactly " + limit + " bans before locking.")
                        );
                        return;
                    }
                } else {
                    // Pick round: only check picks
                    if (pendingPicks.size() != limit) {
                        context.player().sendSystemMessage(
                            Component.literal("You must select exactly " + limit + " picks before locking.")
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
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(player, endPayload);
                    }
                    
                    // Clear the session
                    activePickBanSession = null;
                    
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("Pick/ban session completed! Picks and bans have been finalized.").withColor(0xFFAA00),
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
                    
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(player, updatePayload);
                    }
                    
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("Round " + activePickBanSession.getCurrentRound() + "/" + activePickBanSession.getMaxRounds() + " - " + 
                                   activePickBanSession.getCurrentActiveTeamName() + "'s turn").withColor(0xFFAA00),
                        false
                    );
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CustomBoardPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();

            // Permission checks handled elsewhere; accept for now to allow client-side custom board packets.

            boolean clearBoard = payload.boardOrClear().isEmpty();
            if (clearBoard) {
                CUSTOM_BOARD = null;
                player.sendSystemMessage(Component.literal("Removed custom board."));
            } else {
                // validate board
                List<String> invalidGoals = new ArrayList<>();
                for (Pair<String, String> goal : payload.boardOrClear().get()) {
                    if (!GoalRegistry.INSTANCE.isGoalValid(goal.getA(), goal.getB())) {
                        invalidGoals.add(" - '" + goal.getA() + "'" + ("null".equals(goal.getB()) ? "" : (" with data: '" + goal.getB() + "'")));
                    }
                }
                if (!invalidGoals.isEmpty()) {
                    player.sendSystemMessage(Component.literal("Invalid board. Could not create goals:\n" + String.join("\n", invalidGoals)));
                    return;
                }
                CUSTOM_BOARD = new LockoutBoard(payload.boardOrClear().get());
                player.sendSystemMessage(Component.literal("Set custom board."));
            }
        });
    }

    public static void resetServerForNewWorld() {
        isInitialized = false;
    }

    public static int startPickBanSession(CommandContext<CommandSourceStack> context, Team team1, Team team2) {
        if (activePickBanSession != null) {
            context.getSource().sendFailure(Component.literal("A pick/ban session is already active! Use /CancelPickBanSession to cancel it first."));
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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }

        context.getSource().sendSuccess(() ->
            Component.literal("Started pick/ban session: " + team1.getName() + " vs " + team2.getName()), false
        );
        
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("Pick/ban session started! Round 1/" + activePickBanSession.getMaxRounds() + " - " + team1.getName() + "'s turn").withColor(0x55FF55),
            false
        );

        return 1;
    }

    public static LocateData locateBiome(MinecraftServer server, ResourceKey<Biome> biome) {
        if (BIOME_LOCATE_DATA.containsKey(biome)) return BIOME_LOCATE_DATA.get(biome);

        net.minecraft.core.BlockPos currentPos = server.overworld().getRespawnData().pos();

        var pair = server.overworld().findClosestBiome3d(
                holder -> holder.is(biome),
                currentPos,
                LOCATE_SEARCH,
                32,
                64);

        LocateData data= new LocateData(false,0);
        if (pair != null) {
            int dx = pair.getFirst().getX() - currentPos.getX();
            int dz = pair.getFirst().getZ() - currentPos.getZ();
            int distance = Mth.floor(Math.sqrt(dx * dx + dz * dz));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        BIOME_LOCATE_DATA.put(biome, data);

        return data;
    }

    public static LocateData locateStructure(MinecraftServer server, ResourceKey<Structure> structure) {
        if (STRUCTURE_LOCATE_DATA.containsKey(structure)) return STRUCTURE_LOCATE_DATA.get(structure);

        net.minecraft.core.BlockPos currentPos = server.overworld().getRespawnData().pos();

        Registry<Structure> registry = server.overworld().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> structureList = HolderSet.direct(registry.getOrThrow(structure));

        var pair = server.overworld().getChunkSource().getGenerator().findNearestMapStructure(
                server.overworld(),
                structureList,
                currentPos,
                LOCATE_SEARCH,
                false);

        LocateData data = new LocateData(false, 0);
        if (pair != null) {
            int dx2 = pair.getFirst().getX() - currentPos.getX();
            int dz2 = pair.getFirst().getZ() - currentPos.getZ();
            int distance = Mth.floor(Math.sqrt(dx2 * dx2 + dz2 * dz2));
            if (distance < LOCATE_SEARCH) {
                data = new LocateData(true, distance);
            }
        }
        STRUCTURE_LOCATE_DATA.put(structure, data);

        return data;
    }

    public static int lockoutCommandLogic(CommandContext<CommandSourceStack> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, false);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    public static int blackoutCommandLogic(CommandContext<CommandSourceStack> context) {
        List<LockoutTeamServer> teams = new ArrayList<>();

        int ret = parseArgumentsIntoTeams(teams, context, true);
        if (ret == 0) return 0;

        startLockout(teams);

        return 1;
    }

    public static int lockoutRandomCommandLogic(CommandContext<CommandSourceStack> context) {
        Integer teamCount = null;
        try {
            teamCount = context.getArgument("team count", Integer.class);
        } catch (Exception ignored) {}
        
        List<LockoutTeamServer> teams = createRandomTeams(context, teamCount);
        if (teams == null) return 0;

        startLockout(teams);
        return 1;
    }

    private static List<LockoutTeamServer> createRandomTeams(CommandContext<CommandSourceStack> context, Integer teamCount) {
        PlayerList playerManager = server.getPlayerList();
        List<ServerPlayer> allPlayers = new ArrayList<>(playerManager.getPlayers());
        
        if (allPlayers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No players online to create teams."));
            return null;
        }

        if (allPlayers.size() < 2) {
            context.getSource().sendFailure(Component.literal("Need at least 2 players online for random lockout."));
            return null;
        }

        // Shuffle players
        java.util.Collections.shuffle(allPlayers);
        
        List<LockoutTeamServer> teams = new ArrayList<>();
        
        if (teamCount == null) {
            // Default: Create 1v1 teams (each player on their own team)
            for (int i = 0; i < allPlayers.size(); i++) {
                ServerPlayer player = allPlayers.get(i);
                teams.add(new LockoutTeamServer(
                    List.of(player.getName().getString()),
                    Lockout.COLOR_ORDERS[i % Lockout.COLOR_ORDERS.length],
                    server
                ));
            }
        } else {
            // Create specified number of teams
            if (teamCount > allPlayers.size()) {
                context.getSource().sendFailure(Component.literal("Cannot create " + teamCount + " teams with only " + allPlayers.size() + " players."));
                return null;
            }
            
            // Create lists to hold player names for each team
            List<List<String>> teamPlayerNames = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) {
                teamPlayerNames.add(new ArrayList<>());
            }
            
            // Assign players to teams in round-robin fashion
            for (int i = 0; i < allPlayers.size(); i++) {
                ServerPlayer player = allPlayers.get(i);
                int teamIndex = i % teamCount;
                teamPlayerNames.get(teamIndex).add(player.getName().getString());
            }
            
            // Create teams with the player lists
            for (int i = 0; i < teamCount; i++) {
                teams.add(new LockoutTeamServer(
                    teamPlayerNames.get(i),
                    Lockout.COLOR_ORDERS[i % Lockout.COLOR_ORDERS.length],
                    server
                ));
            }
        }
        
        return teams;
    }

    private static void startLockout(List<LockoutTeamServer> teams) {
        // Clear old runnables
        gameStartRunnables.clear();

        PlayerList playerManager = server.getPlayerList();
        List<ServerPlayer> allServerPlayers = playerManager.getPlayers();
        List<UUID> allLockoutPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .toList();
        List<UUID> allSpectatorPlayers = allServerPlayers.stream()
                .map(ServerPlayer::getUUID)
                .filter(uuid -> !allLockoutPlayers.contains(uuid))
                .toList();

        for (ServerPlayer serverPlayer : allServerPlayers) {
            serverPlayer.getInventory().clearContent();
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
            serverPlayer.removeAllEffects();
            serverPlayer.getFoodData().setSaturation(5);
            serverPlayer.getFoodData().setFoodLevel(20);
            serverPlayer.getFoodData().exhaustionLevel = 0.0f;
            serverPlayer.setExperienceLevels(0);
            serverPlayer.setExperiencePoints(0);
            serverPlayer.clearFire();

            // Clear all stats
            for (@SuppressWarnings("unchecked") StatType<Object> statType : new StatType[]{Stats.ITEM_CRAFTED, Stats.BLOCK_MINED, Stats.ITEM_USED, Stats.ITEM_BROKEN, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED, Stats.ENTITY_KILLED, Stats.ENTITY_KILLED_BY, Stats.CUSTOM}) {
                for (Object value : statType.getRegistry()) {
                    serverPlayer.resetStat(statType.get(value));
                }
            }
            serverPlayer.getStats().sendStats(serverPlayer);
            // Clear all advancements
            AdvancementCommands.Action.REVOKE.perform(serverPlayer, server.getAdvancements().getAllAdvancements(), false);

            if (allLockoutPlayers.contains(serverPlayer.getUUID())) {
                serverPlayer.gameMode.setGameModeForPlayer(GameType.ADVENTURE, null);
            } else {
                serverPlayer.gameMode.setGameModeForPlayer(GameType.SPECTATOR, null);
                serverPlayer.sendSystemMessage(Component.literal("You are spectating this match.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }

        ServerLevel world = server.getLevel(Level.OVERWORLD);

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
                    ServerPlayer player = playerManager.getPlayer(playerUuid);
                    if (player != null) {
                        player.sendSystemMessage(Component.literal(errorMessage).withStyle(ChatFormatting.RED));
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

        // Delay tooltip sending by 1 tick to ensure lockout is fully initialized
        ((LockoutRunnable) () -> {
            List<Goal> tooltipGoals = new ArrayList<>(lockout.getBoard().getGoals()).stream().filter(g -> g instanceof HasTooltipInfo).toList();
            for (Goal goal : tooltipGoals) {
                // Update teams tooltip
                for (LockoutTeam team : lockout.getTeams()) {
                    ((LockoutTeamServer) team).sendTooltipUpdate((Goal & HasTooltipInfo) goal, false);
                }
                // Update spectator tooltip
                if (!allSpectatorPlayers.isEmpty()) {
                    List<String> spectatorTooltip = ((HasTooltipInfo) goal).getSpectatorTooltip();
                    if (spectatorTooltip != null && !spectatorTooltip.isEmpty()) {
                        var payload = new UpdateTooltipPayload(goal.getId(), String.join("\n", spectatorTooltip));
                        for (UUID spectator : allSpectatorPlayers) {
                            ServerPlayer spectatorPlayer = playerManager.getPlayer(spectator);
                            if (spectatorPlayer != null) {
                                ServerPlayNetworking.send(spectatorPlayer, payload);
                            }
                        }
                    }
                }
            }
        }).runTaskAfter(1L);

        for (ServerPlayer player : allServerPlayers) {
            ServerPlayNetworking.send(player, lockout.getTeamsGoalsPacket());
            ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());

            if (!lockout.isSoloBlackout() && lockout.isLockoutPlayer(player.getUUID())) {
                player.addItem(compassHandler.newCompass());
            }
        }

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");

        for (int i = 3; i >= 0; i--) {
            if (i > 0) {
                final int secs = i;
                ((LockoutRunnable) () -> {
                    playerManager.broadcastSystemMessage(Component.literal("Starting in " + secs + "..."), false);
                }).runTaskAfter(20L * (lockoutStartTime - i));
            } else {
                ((LockoutRunnable) () -> {
                    lockout.setStarted(true);

                    for (ServerPlayer player : allServerPlayers) {
                        if (player == null) continue;
                        ServerPlayNetworking.send(player, StartLockoutPayload.INSTANCE);
                        if (allLockoutPlayers.contains(player.getUUID())) {
                            player.gameMode.setGameModeForPlayer(GameType.SURVIVAL, null);

                            // Update waypoint color to match team color with variation for team members
                            LockoutTeam playerTeam = lockout.getPlayerTeam(player.getUUID());
                            if (playerTeam != null) {
                            // Find player index within their team
                            updatePlayerWaypointColor(player, playerTeam.getColor());                                
                            }

                        }
                    }
                    server.getPlayerList().broadcastSystemMessage(Component.literal(lockout.getModeName() + " has begun."), false);
                }).runTaskAfter(20L * lockoutStartTime);
            }
        }
    }


    public static void updatePlayerWaypointColor(ServerPlayer player, ChatFormatting teamColor) {
        try {
            int colorValue = TeamColor.valueOf(teamColor.name()).rgb();

            // Create slight color variation for team members
            String hexColor = String.format("%06X", colorValue & 0xFFFFFF);
            
            String command = String.format("waypoint modify %s color hex %s", player.getName().getString(), hexColor);

            // Create command source with appropriate permissions and silent execution
            CommandSourceStack commandSource = new CommandSourceStack(
                net.minecraft.commands.CommandSource.NULL,
                player.position(),
                new net.minecraft.world.phys.Vec2(player.getXRot(), player.getYRot()),
                (ServerLevel) player.level(),
                PermissionSet.ALL_PERMISSIONS,
                player.getName().getString(),
                Component.empty(),
                server,
                player
            );

            // Parse and execute the command
            server.getCommands().performPrefixedCommand(commandSource, command);
        } catch (Exception e) {
            // Silently ignore errors to avoid disrupting game start
            // Waypoint modification is not critical for game functionality            // Waypoint modification is not critical for game functionality
        }
    }

    /**
     * Creates a slight color variation for team members
     * @param baseColor The base team color
     * @param playerIndex The index of the player within their team
     * @return Modified color with slight variation
     */
    private static int createColorVariation(int baseColor, int playerIndex) {
        if (playerIndex == 0) {
            return baseColor; // First player gets the original team color
        }

        // Extract RGB components
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        // Create variation based on player index
        // Use different multipliers for each component to create noticeable but subtle differences
        double variation = 0.15; // 15% variation
        int variationAmount = (int) (variation * 255);

        // Apply different variations based on player index
        switch (playerIndex % 4) {
            case 1: // Slightly brighter
                r = Math.min(255, r + variationAmount);
                g = Math.min(255, g + variationAmount);
                b = Math.min(255, b + variationAmount);
                break;
            case 2: // Slightly darker
                r = Math.max(0, r - variationAmount);
                g = Math.max(0, g - variationAmount);
                b = Math.max(0, b - variationAmount);
                break;
            case 3: // Slightly more saturated (boost dominant color)
                int maxComponent = Math.max(Math.max(r, g), b);
                if (maxComponent == r) {
                    r = Math.min(255, r + variationAmount);
                } else if (maxComponent == g) {
                    g = Math.min(255, g + variationAmount);
                } else {
                    b = Math.min(255, b + variationAmount);
                }
                break;
        }

        return (r << 16) | (g << 8) | b;
    }

    private static int parseArgumentsIntoTeams(List<LockoutTeamServer> teams, CommandContext<CommandSourceStack> context, boolean isBlackout) {
        String argument = null;

        PlayerList playerManager = server.getPlayerList();

        try {
            argument = context.getArgument("player names", String.class);
            String[] playerNames = argument.split(" +");
            
            if (isBlackout) {
                if (playerNames.length == 0) {
                    context.getSource().sendFailure(Component.literal("Not enough players listed."));
                    return 0;
                }

                List<String> validPlayerNames = new ArrayList<>();
                for (String playerName : playerNames) {
                    ServerPlayer player = playerManager.getPlayer(playerName);
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("Player " + playerName + " is not online."));
                        return 0;
                    }
                    validPlayerNames.add(player.getName().getString());
                }
                teams.add(new LockoutTeamServer(validPlayerNames, Lockout.COLOR_ORDERS[0], server));
                return 1;

            } else {
                if (playerNames.length < 2) {
                    context.getSource().sendFailure(Component.literal("Not enough players listed. You need at least 2 players."));
                    return 0;
                }
                if (playerNames.length > 16) {
                    context.getSource().sendFailure(Component.literal("Too many players listed."));
                    return 0;
                }

                for (int i = 0; i < playerNames.length; i++) {
                    String playerName = playerNames[i];
                    ServerPlayer player = playerManager.getPlayer(playerName);
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("Player " + playerName + " is not online."));
                        return 0;
                    }
                    teams.add(new LockoutTeamServer(List.of(player.getName().getString()), Lockout.COLOR_ORDERS[i], server));
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
                        context.getSource().sendFailure(Component.literal("Not enough teams listed."));
                        return 0;
                    }
                    if (teamNames.length > 1) {
                        context.getSource().sendFailure(Component.literal("Only one team can play Blackout."));
                        return 0;
                    }
                } else {
                    if (teamNames.length < 2) {
                        context.getSource().sendFailure(Component.literal("Not enough teams listed. Make sure you separate team names with spaces."));
                        return 0;
                    }
                    if (teamNames.length > 16) {
                        context.getSource().sendFailure(Component.literal("Too many teams listed."));
                        return 0;
                    }
                }

                List<PlayerTeam> scoreboardTeams = new ArrayList<>();
                for (String teamName : teamNames) {
                    PlayerTeam team = scoreboard.getPlayerTeam(teamName);
                    if (team == null) {
                        context.getSource().sendFailure(Component.literal("Team " + teamName + " is invalid."));
                        return 0;
                    }
                    for (String player : team.getPlayers()) {
                        if (playerManager.getPlayer(player) == null) {
                            context.getSource().sendFailure(Component.literal("Player " + player + " on team " + teamName + " is invalid. Remove them from the team and try again."));
                            return 0;
                        }
                    }
                    scoreboardTeams.add(team);
                }
                for (PlayerTeam team : scoreboardTeams) {
                    if (team.getPlayers().isEmpty()) {
                        context.getSource().sendFailure(Component.literal("Team " + team.getName() + " doesn't have any players."));
                        return 0;
                    }
                    ChatFormatting teamColor = team.getColor()
                        .map(tc -> ChatFormatting.valueOf(tc.name()))
                        .orElse(null);
                    if (teamColor == null || teamHasColor(teams, teamColor)) {
                        // Select an available color.
                        boolean found = false;
                        for (ChatFormatting colorOrder : Lockout.COLOR_ORDERS) {
                            if (!teamHasColor(teams, colorOrder)) {
                                found = true;
                                teamColor = colorOrder;
                                team.setColor(Optional.of(TeamColor.valueOf(colorOrder.name())));
                                break;
                            }
                        }
                        if (!found) {
                            context.getSource().sendFailure(Component.literal("Could not find assignable color for team " + team.getName() + ". Try recreating teams."));
                            return 0;
                        }
                    }
                    List<String> actualPlayerNames = new ArrayList<>();
                    for (String playerName : team.getPlayers()) {
                        actualPlayerNames.add(playerManager.getPlayer(playerName).getName().getString());
                    }
                    teams.add(new LockoutTeamServer(new ArrayList<>(actualPlayerNames), teamColor, server));
                }
            } catch (Exception ignored) {}
        }

        if (argument == null) {
            context.getSource().sendFailure(Component.literal("Illegal argument."));
            return 0;
        }
        return 1;
    }

    private static boolean teamHasColor(List<LockoutTeamServer> teams, ChatFormatting color) {
        for (LockoutTeam lockoutTeam : teams) {
            if (lockoutTeam.getColor() == color) {
                return true;
            }
        }
        return false;
    }

    public static int setChat(CommandContext<CommandSourceStack> context, ChatManager.Type type) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This is a player-only command."));
            return 0;
        }

        ChatManager.Type curr = ChatManager.getChat(player);
        if (curr == type) {
            player.sendSystemMessage(Component.literal("You are already chatting in " + type.name() + "."));
        } else {
            player.sendSystemMessage(Component.literal("You are now chatting in " + type.name() + "."));
            ChatManager.setChat(player, type);
        }
        return 1;
    }

    public static int viewStatistics(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This is a player-only command."));
            return 0;
        }
        
        if (lockout != null && lockout.getStatistics() != null) {
            lockout.getStatistics().showFullStatistics(player);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("No statistics available."));
            return 0;
        }
    }

    public static int downloadStatistics(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This is a player-only command."));
            return 0;
        }

        if (lockout != null && lockout.getStatistics() != null) {
            // Generate statistics content and send to client for local saving
            String[] fileData = lockout.getStatistics().generateStatisticsContent();
            String filename = fileData[0];
            String content = fileData[1];
            
            me.marin.lockout.network.DownloadStatisticsPayload payload = 
                new me.marin.lockout.network.DownloadStatisticsPayload(filename, content);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("No statistics available."));
            return 0;
        }
    }

    public static int giveGoal(CommandContext<CommandSourceStack> context) {
        try {
            if (!Lockout.isLockoutRunning(lockout)) {
                context.getSource().sendFailure(Component.literal("There's no active lockout match."));
                return 0;
            }

            int idx = context.getArgument("goal number", Integer.class);

            Collection<NameAndId> playerConfigs;
            try {
                playerConfigs = GameProfileArgument.getGameProfiles(context, "player name");
            } catch (CommandSyntaxException e) {
                context.getSource().sendFailure(Component.literal("Invalid target."));
                return 0;
            }

            if (playerConfigs.size() != 1) {
                context.getSource().sendFailure(Component.literal("Invalid number of targets."));
                return 0;
            }
            NameAndId playerConfig = playerConfigs.stream().findFirst().get();
            if (!lockout.isLockoutPlayer(playerConfig.id())) {
                context.getSource().sendFailure(Component.literal("Player " + playerConfig.name() + " is not playing Lockout."));
                return 0;
            }

            if (idx > lockout.getBoard().getGoals().size()) {
                context.getSource().sendFailure(Component.literal("Goal number does not exist on the board."));
                return 0;
            }
            Goal goal = lockout.getBoard().getGoals().get(idx - 1);

            context.getSource().sendSuccess(() -> Component.literal("Gave " + playerConfig.name() + " goal \"" + goal.getGoalName() + "\"."), false);
            lockout.updateGoalCompletion(goal, playerConfig.id());
            return 1;
        } catch (RuntimeException e) {
            Lockout.error(e);
            return 0;
        }
    }

    public static int setStartTime(CommandContext<CommandSourceStack> context) {
        int seconds = context.getArgument("seconds", Integer.class);

        lockoutStartTime = seconds;
        context.getSource().sendSuccess(() -> Component.literal("Updated start time to " + seconds + "s."), false);
        return 1;
    }

    public static int setGracePeriod(CommandContext<CommandSourceStack> context) {
        int seconds = context.getArgument("seconds", Integer.class);

        gracePeriodSeconds = seconds;
        context.getSource().sendSuccess(() -> Component.literal("Updated grace period to " + seconds + "s."), false);
        return 1;
    }

    public static int getGracePeriodSeconds() {
        return gracePeriodSeconds;
    }

    public static int setBoardSize(CommandContext<CommandSourceStack> context) {
        int size = context.getArgument("board size", Integer.class);

        boardSize = size;
        context.getSource().sendSuccess(() -> Component.literal("Updated board size to " + size + "."), false);
        return 1;
    }

}