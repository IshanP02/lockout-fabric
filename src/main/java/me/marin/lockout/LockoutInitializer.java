package me.marin.lockout;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.marin.lockout.lockout.DefaultGoalRegister;
import me.marin.lockout.network.CustomBoardPayload;
import me.marin.lockout.network.EndPickBanSessionPayload;
import me.marin.lockout.network.Networking;
import me.marin.lockout.network.UpdatePickBanSessionPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.util.PlayerSuggestionProvider;
import me.marin.lockout.util.TeamSuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.function.SetPotionLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import me.marin.lockout.generator.GoalGroup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static me.marin.lockout.Constants.MAX_BOARD_SIZE;
import static me.marin.lockout.Constants.MIN_BOARD_SIZE;
import static me.marin.lockout.Constants.NAMESPACE;

public class LockoutInitializer implements ModInitializer {

    private static final Predicate<ServerCommandSource> PERMISSIONS = (ssc) -> ssc.hasPermissionLevel(2) || ssc.getServer().isSingleplayer();

    public static Version MOD_VERSION;

    @Override
    public void onInitialize() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(NAMESPACE).get().getMetadata().getVersion();

        LockoutConfig.load();
        Networking.registerPayloads();
        DefaultGoalRegister.registerGoals();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            {
                {
                    // Lockout command
                    var commandNode = CommandManager.literal("lockout").requires(PERMISSIONS).build();
                    var teamsNode = CommandManager.literal("teams").build();
                    var playersNode = CommandManager.literal("players").build();
                    var randomNode = CommandManager.literal("random").executes(LockoutServer::lockoutRandomCommandLogic).build();
                    var teamCountNode = CommandManager.argument("team count", IntegerArgumentType.integer(2, 16)).executes(LockoutServer::lockoutRandomCommandLogic).build();
                    //TODO make custom argument types
                    var teamListNode = CommandManager.argument("team names", StringArgumentType.greedyString()).suggests(new TeamSuggestionProvider()).executes(LockoutServer::lockoutCommandLogic).build();
                    var playerListNode = CommandManager.argument("player names", StringArgumentType.greedyString()).suggests(new PlayerSuggestionProvider()).executes(LockoutServer::lockoutCommandLogic).build();

                    dispatcher.getRoot().addChild(commandNode);
                    commandNode.addChild(teamsNode);
                    commandNode.addChild(playersNode);
                    commandNode.addChild(randomNode);
                    randomNode.addChild(teamCountNode);
                    teamsNode.addChild(teamListNode);
                    playersNode.addChild(playerListNode);
                }


                {
                    // Blackout command
                    var commandNode = CommandManager.literal("blackout").requires(PERMISSIONS).build();
                    var teamNode = CommandManager.literal("team").build();
                    var playersNode = CommandManager.literal("players").build();
                    //TODO make custom argument types
                    var teamNameNode = CommandManager.argument("team name", StringArgumentType.greedyString()).suggests(new TeamSuggestionProvider()).executes(LockoutServer::blackoutCommandLogic).build();
                    var playerListNode = CommandManager.argument("player names", StringArgumentType.greedyString()).suggests(new PlayerSuggestionProvider()).executes(LockoutServer::blackoutCommandLogic).build();
                    dispatcher.getRoot().addChild(commandNode);
                    commandNode.addChild(teamNode);
                    commandNode.addChild(playersNode);
                    teamNode.addChild(teamNameNode);
                    playersNode.addChild(playerListNode);
                }
            }


            {
                // Chat command
                var chatCommandNode = CommandManager.literal("chat").build();
                var chatTeamNode = CommandManager.literal("team").executes(context -> LockoutServer.setChat(context, ChatManager.Type.TEAM)).build();
                var chatLocalNode = CommandManager.literal("local").executes(context -> LockoutServer.setChat(context, ChatManager.Type.LOCAL)).build();

                dispatcher.getRoot().addChild(chatCommandNode);
                chatCommandNode.addChild(chatTeamNode);
                chatCommandNode.addChild(chatLocalNode);
            }


            {
                // GiveGoal command
                var giveGoalRoot = CommandManager.literal("GiveGoal").requires(PERMISSIONS).build();
                var playerName = CommandManager.argument("player name", GameProfileArgumentType.gameProfile()).build();
                var goalIndex = CommandManager.argument("goal number", IntegerArgumentType.integer(1, MAX_BOARD_SIZE * MAX_BOARD_SIZE)).executes(LockoutServer::giveGoal).build();

                dispatcher.getRoot().addChild(giveGoalRoot);
                giveGoalRoot.addChild(playerName);
                playerName.addChild(goalIndex);
            }

            {
                // SetStartTime command
                var setStartTimeRoot = CommandManager.literal("SetStartTime").requires(PERMISSIONS).build();
                var seconds = CommandManager.argument("seconds", IntegerArgumentType.integer(5, 300)).executes(LockoutServer::setStartTime).build();

                dispatcher.getRoot().addChild(setStartTimeRoot);
                setStartTimeRoot.addChild(seconds);
            }

            {
                // RemoveCustomBoard command (SetCustomBoard is registered in LockoutClient, and server listens for a packet)

                dispatcher.getRoot().addChild(CommandManager.literal("RemoveCustomBoard").requires(PERMISSIONS).executes((context) -> {
                    ClientPlayNetworking.send(new CustomBoardPayload(Optional.empty()));
                    return 1;
                }).build());
            }

            {
                // SetBoardSize command

                var setBoardTimeRoot = CommandManager.literal("SetBoardSize").requires(PERMISSIONS).build();
                var size = CommandManager.argument("board size", IntegerArgumentType.integer(MIN_BOARD_SIZE, MAX_BOARD_SIZE)).executes(LockoutServer::setBoardSize).build();

                dispatcher.getRoot().addChild(setBoardTimeRoot);
                setBoardTimeRoot.addChild(size);
            }

            {
                // RemovePicks command
                dispatcher.getRoot().addChild(CommandManager.literal("RemovePicks").requires(PERMISSIONS).executes((context) -> {
                    // Remove goal-to-player mappings for picks before clearing
                    for (String goalId : GoalGroup.PICKS.getGoals()) {
                        GoalGroup.setGoalPlayer(goalId, null);
                    }
                    GoalGroup.PICKS.getGoals().clear();
                    LockoutServer.SERVER_PICKS.clear();
                    context.getSource().sendMessage(Text.literal("Removed picks."));
                    
                    // Broadcast update to all players on server
                    if (context.getSource().getServer() != null) {
                        // Build remaining goal-to-player map (only bans remain)
                        java.util.Map<String, String> goalToPlayerMap = new java.util.HashMap<>();
                        for (String goalId : LockoutServer.SERVER_BANS) {
                            String playerName = GoalGroup.getGoalPlayer(goalId);
                            if (playerName != null) {
                                goalToPlayerMap.put(goalId, playerName);
                            }
                        }
                        
                        var payload = new UpdatePicksBansPayload(
                            new java.util.ArrayList<>(LockoutServer.SERVER_PICKS),
                            new java.util.ArrayList<>(LockoutServer.SERVER_BANS),
                            goalToPlayerMap
                        );
                        for (var player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                    return 1;
                }).build());
            }

            {
                // RemoveBans command
                dispatcher.getRoot().addChild(CommandManager.literal("RemoveBans").requires(PERMISSIONS).executes((context) -> {
                    // Remove goal-to-player mappings for bans before clearing
                    for (String goalId : GoalGroup.BANS.getGoals()) {
                        GoalGroup.setGoalPlayer(goalId, null);
                    }
                    GoalGroup.BANS.getGoals().clear();
                    LockoutServer.SERVER_BANS.clear();
                    context.getSource().sendMessage(Text.literal("Removed bans."));
                    
                    // Broadcast update to all players on server
                    if (context.getSource().getServer() != null) {
                        // Build remaining goal-to-player map (only picks remain)
                        java.util.Map<String, String> goalToPlayerMap = new java.util.HashMap<>();
                        for (String goalId : LockoutServer.SERVER_PICKS) {
                            String playerName = GoalGroup.getGoalPlayer(goalId);
                            if (playerName != null) {
                                goalToPlayerMap.put(goalId, playerName);
                            }
                        }
                        
                        var payload = new UpdatePicksBansPayload(
                            new java.util.ArrayList<>(LockoutServer.SERVER_PICKS),
                            new java.util.ArrayList<>(LockoutServer.SERVER_BANS),
                            goalToPlayerMap
                        );
                        for (var player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                    return 1;
                }).build());
            }

            {
                // SimulatePickBans command
                var simulatePickBansRoot = CommandManager.literal("SimulatePickBans").requires(PERMISSIONS).build();
                var team1Arg = CommandManager.argument("team1", StringArgumentType.word()).suggests(new TeamSuggestionProvider()).build();
                var team2Arg = CommandManager.argument("team2", StringArgumentType.word()).suggests(new TeamSuggestionProvider()).executes(context -> {
                    String team1Name = StringArgumentType.getString(context, "team1");
                    String team2Name = StringArgumentType.getString(context, "team2");
                    
                    ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
                    Team team1 = scoreboard.getTeam(team1Name);
                    Team team2 = scoreboard.getTeam(team2Name);
                    
                    if (team1 == null) {
                        context.getSource().sendError(Text.literal("Team " + team1Name + " does not exist."));
                        return 0;
                    }
                    if (team2 == null) {
                        context.getSource().sendError(Text.literal("Team " + team2Name + " does not exist."));
                        return 0;
                    }
                    if (team1.equals(team2)) {
                        context.getSource().sendError(Text.literal("Cannot start pick/ban with the same team twice."));
                        return 0;
                    }
                    
                    return LockoutServer.startPickBanSession(context, team1, team2);
                }).build();

                dispatcher.getRoot().addChild(simulatePickBansRoot);
                simulatePickBansRoot.addChild(team1Arg);
                team1Arg.addChild(team2Arg);
            }

            {
                // CancelPickBanSession command
                dispatcher.getRoot().addChild(
                    CommandManager.literal("CancelPickBanSession")
                        .requires(PERMISSIONS)
                        .executes(context -> {
                            if (LockoutServer.activePickBanSession == null) {
                                context.getSource().sendError(Text.literal("No active pick/ban session to cancel."));
                                return 0;
                            }
                            
                            // Clear all goal groups on server
                            GoalGroup.PICKS.getGoals().clear();
                            GoalGroup.BANS.getGoals().clear();
                            GoalGroup.PENDING_PICKS.getGoals().clear();
                            GoalGroup.PENDING_BANS.getGoals().clear();
                            GoalGroup.clearGoalPlayers();
                            
                            // Clear the session
                            LockoutServer.activePickBanSession = null;
                            
                            // Broadcast cancellation to all players
                            EndPickBanSessionPayload payload = new EndPickBanSessionPayload(
                                true,
                                new java.util.HashSet<>(),
                                new java.util.HashSet<>(),
                                new java.util.HashMap<>()
                            );
                            for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                                ServerPlayNetworking.send(player, payload);
                            }
                            
                            context.getSource().sendMessage(Text.literal("Pick/ban session cancelled."));
                            return 1;
                        })
                        .build()
                );
            }

            {
                // PickBanSelectionLimit command
                dispatcher.getRoot().addChild(
                    CommandManager.literal("PickBanSelectionLimit")
                        .requires(PERMISSIONS)
                        .then(CommandManager.argument("limit", IntegerArgumentType.integer(1, 5))
                            .executes(context -> {
                                int limit = IntegerArgumentType.getInteger(context, "limit");
                                
                                if (LockoutServer.activePickBanSession == null) {
                                    // If no active session, store the limit for the next session
                                    LockoutServer.defaultPickBanLimit = limit;
                                    context.getSource().sendMessage(Text.literal("Pick/ban selection limit set to " + limit + " for the next session."));
                                    return 1;
                                }
                                
                                // Can only change limit before any rounds are locked
                                if (LockoutServer.activePickBanSession.getCurrentRound() > 1) {
                                    context.getSource().sendError(Text.literal("Cannot change selection limit after round 1 has started."));
                                    return 0;
                                }
                                
                                LockoutServer.activePickBanSession.setSelectionLimit(limit);
                                
                                // Use goal-to-player map from the session
                                java.util.Map<String, String> goalToPlayerMap = LockoutServer.activePickBanSession.getGoalToPlayerMap();
                                
                                // Broadcast the new limit to all players
                                UpdatePickBanSessionPayload payload = new UpdatePickBanSessionPayload(
                                    LockoutServer.activePickBanSession.getCurrentRound(),
                                    LockoutServer.activePickBanSession.isTeam1Turn(),
                                    LockoutServer.activePickBanSession.getTeam1Name(),
                                    LockoutServer.activePickBanSession.getTeam2Name(),
                                    LockoutServer.activePickBanSession.getAllLockedPicks(),
                                    LockoutServer.activePickBanSession.getAllLockedBans(),
                                    LockoutServer.activePickBanSession.getPendingPicks(),
                                    LockoutServer.activePickBanSession.getPendingBans(),
                                    LockoutServer.activePickBanSession.getSelectionLimit(),
                                    goalToPlayerMap,
                                    LockoutServer.activePickBanSession.getMaxRounds()
                                );
                                
                                for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                                    ServerPlayNetworking.send(player, payload);
                                }
                                
                                context.getSource().sendMessage(Text.literal("Pick/ban selection limit set to " + limit));
                                return 1;
                            })
                        )
                        .build()
                );
            }

            {
                // MaxRounds command
                dispatcher.getRoot().addChild(
                    CommandManager.literal("MaxRounds")
                        .requires(PERMISSIONS)
                        .then(CommandManager.argument("rounds", IntegerArgumentType.integer(2, 10))
                            .executes(context -> {
                                int rounds = IntegerArgumentType.getInteger(context, "rounds");
                                
                                if (rounds % 2 != 0) {
                                    context.getSource().sendError(Text.literal("Max rounds must be an even number."));
                                    return 0;
                                }
                                
                                if (LockoutServer.activePickBanSession != null) {
                                    context.getSource().sendError(Text.literal("Cannot change max rounds during an active session."));
                                    return 0;
                                }
                                
                                LockoutServer.defaultMaxRounds = rounds;
                                context.getSource().sendMessage(Text.literal("Max rounds set to " + rounds + " for the next session."));
                                return 1;
                            })
                        )
                        .build()
                );
            }

        });

        LootTableEvents.REPLACE.register(((key, original, source, registries) -> {
            if (Objects.equals(key, LootTables.PIGLIN_BARTERING_GAMEPLAY)) {
                UniformLootNumberProvider ironNuggetsCount = UniformLootNumberProvider.create(9.0F, 36.0F);
                UniformLootNumberProvider quartzCount = UniformLootNumberProvider.create(8.0F, 16.0F);
                UniformLootNumberProvider glowstoneDustCount = UniformLootNumberProvider.create(5.0F, 12.0F);
                UniformLootNumberProvider magmaCreamCount = UniformLootNumberProvider.create(2.0F, 6.0F);
                UniformLootNumberProvider enderPearlCount = UniformLootNumberProvider.create(4.0F, 8.0F);
                UniformLootNumberProvider stringCount = UniformLootNumberProvider.create(8.0F, 24.0F);
                UniformLootNumberProvider fireChargeCount = UniformLootNumberProvider.create(1.0F, 5.0F);
                UniformLootNumberProvider gravelCount = UniformLootNumberProvider.create(8.0F, 16.0F);
                UniformLootNumberProvider leatherCount = UniformLootNumberProvider.create(4.0F, 10.0F);
                UniformLootNumberProvider netherBrickCount = UniformLootNumberProvider.create(4.0F, 16.0F);
                UniformLootNumberProvider cryingObsidianCount = UniformLootNumberProvider.create(1.0F, 3.0F);
                UniformLootNumberProvider soulSandCount = UniformLootNumberProvider.create(4.0F, 16.0F);

                LootPool pool = LootPool.builder()
                        .with(ItemEntry.builder(Items.BOOK).apply(EnchantRandomlyLootFunction.create().option(registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SOUL_SPEED))).weight(5))
                        .with(ItemEntry.builder(Items.IRON_BOOTS).apply(EnchantRandomlyLootFunction.create().option(registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SOUL_SPEED))).weight(8))
                        .with(ItemEntry.builder(Items.POTION).apply(SetPotionLootFunction.builder(Potions.FIRE_RESISTANCE)).weight(10))
                        .with(ItemEntry.builder(Items.SPLASH_POTION).apply(SetPotionLootFunction.builder(Potions.FIRE_RESISTANCE)).weight(10))
                        .with(ItemEntry.builder(Items.IRON_NUGGET).apply(SetCountLootFunction.builder(ironNuggetsCount)).weight(10))
                        .with(ItemEntry.builder(Items.QUARTZ).apply(SetCountLootFunction.builder(quartzCount)).weight(20))
                        .with(ItemEntry.builder(Items.GLOWSTONE_DUST).apply(SetCountLootFunction.builder(glowstoneDustCount)).weight(20))
                        .with(ItemEntry.builder(Items.MAGMA_CREAM).apply(SetCountLootFunction.builder(magmaCreamCount)).weight(20))
                        .with(ItemEntry.builder(Items.ENDER_PEARL).apply(SetCountLootFunction.builder(enderPearlCount)).weight(20))
                        .with(ItemEntry.builder(Items.STRING).apply(SetCountLootFunction.builder(stringCount)).weight(20))
                        .with(ItemEntry.builder(Items.FIRE_CHARGE).apply(SetCountLootFunction.builder(fireChargeCount)).weight(40))
                        .with(ItemEntry.builder(Items.GRAVEL).apply(SetCountLootFunction.builder(gravelCount)).weight(40))
                        .with(ItemEntry.builder(Items.LEATHER).apply(SetCountLootFunction.builder(leatherCount)).weight(40))
                        .with(ItemEntry.builder(Items.NETHER_BRICK).apply(SetCountLootFunction.builder(netherBrickCount)).weight(40))
                        .with(ItemEntry.builder(Items.OBSIDIAN).weight(40))
                        .with(ItemEntry.builder(Items.CRYING_OBSIDIAN).apply(SetCountLootFunction.builder(cryingObsidianCount)).weight(40))
                        .with(ItemEntry.builder(Items.SOUL_SAND).apply(SetCountLootFunction.builder(soulSandCount)).weight(40))
                        .build();
                return LootTable.builder().pool(pool).build();
            }
            return null;
        }));

    }

}
