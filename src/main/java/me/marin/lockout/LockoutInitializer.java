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
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import me.marin.lockout.generator.GoalGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Predicate;

import static me.marin.lockout.Constants.*;

public class LockoutInitializer implements ModInitializer {

    private static final Predicate<CommandSourceStack> PERMISSIONS = ssc -> Commands.LEVEL_GAMEMASTERS.check(ssc.permissions());

    public static Version MOD_VERSION;

    @Override
    public void onInitialize() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(NAMESPACE).get().getMetadata().getVersion();

        LockoutConfig.load();
        Networking.registerPayloads();
        DefaultGoalRegister.registerGoals();

        // Register server event handlers once when mod initializes (not every world load)
        LockoutServer.registerServerEventHandlers();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            {
                {
                    // Lockout command
                    var commandNode = Commands.literal("lockout").requires(PERMISSIONS).build();
                    var teamsNode = Commands.literal("teams").build();
                    var playersNode = Commands.literal("players").build();
                    var randomNode = Commands.literal("random").executes(LockoutServer::lockoutRandomCommandLogic).build();
                    var teamCountNode = Commands.argument("team count", IntegerArgumentType.integer(2, 16)).executes(LockoutServer::lockoutRandomCommandLogic).build();
                    //TODO make custom argument types
                    var teamListNode = Commands.argument("team names", StringArgumentType.greedyString()).suggests(new TeamSuggestionProvider()).executes(LockoutServer::lockoutCommandLogic).build();
                    var playerListNode = Commands.argument("player names", StringArgumentType.greedyString()).suggests(new PlayerSuggestionProvider()).executes(LockoutServer::lockoutCommandLogic).build();

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
                    var commandNode = Commands.literal("blackout").requires(PERMISSIONS).build();
                    var teamNode = Commands.literal("team").build();
                    var playersNode = Commands.literal("players").build();
                    //TODO make custom argument types
                    var teamNameNode = Commands.argument("team name", StringArgumentType.greedyString()).suggests(new TeamSuggestionProvider()).executes(LockoutServer::blackoutCommandLogic).build();
                    var playerListNode = Commands.argument("player names", StringArgumentType.greedyString()).suggests(new PlayerSuggestionProvider()).executes(LockoutServer::blackoutCommandLogic).build();
                    dispatcher.getRoot().addChild(commandNode);
                    commandNode.addChild(teamNode);
                    commandNode.addChild(playersNode);
                    teamNode.addChild(teamNameNode);
                    playersNode.addChild(playerListNode);
                }
            }


            {
                // Chat command
                var chatCommandNode = Commands.literal("chat").build();
                var chatTeamNode = Commands.literal("team").executes(context -> LockoutServer.setChat(context, ChatManager.Type.TEAM)).build();
                var chatLocalNode = Commands.literal("local").executes(context -> LockoutServer.setChat(context, ChatManager.Type.LOCAL)).build();

                dispatcher.getRoot().addChild(chatCommandNode);
                chatCommandNode.addChild(chatTeamNode);
                chatCommandNode.addChild(chatLocalNode);
            }

            {
                // Game statistics command
                var lockoutStatsRoot = Commands.literal("GameStatistics").build();
                var viewNode = Commands.literal("view").executes((context) -> LockoutServer.viewStatistics(context)).build();
                var downloadNode = Commands.literal("download").executes((context) -> LockoutServer.downloadStatistics(context)).build();

                dispatcher.getRoot().addChild(lockoutStatsRoot);
                lockoutStatsRoot.addChild(viewNode);
                lockoutStatsRoot.addChild(downloadNode);
            }


            {
                // GiveGoal command
                var giveGoalRoot = Commands.literal("GiveGoal").requires(PERMISSIONS).build();
                var playerName = Commands.argument("player name", GameProfileArgument.gameProfile()).build();
                var goalIndex = Commands.argument("goal number", IntegerArgumentType.integer(1, MAX_BOARD_SIZE * MAX_BOARD_SIZE)).executes(LockoutServer::giveGoal).build();

                dispatcher.getRoot().addChild(giveGoalRoot);
                giveGoalRoot.addChild(playerName);
                playerName.addChild(goalIndex);
            }

            {
                // SetStartTime command
                var setStartTimeRoot = Commands.literal("SetStartTime").requires(PERMISSIONS).build();
                var seconds = Commands.argument("seconds", IntegerArgumentType.integer(5, 300)).executes(LockoutServer::setStartTime).build();

                dispatcher.getRoot().addChild(setStartTimeRoot);
                setStartTimeRoot.addChild(seconds);
            }

            {
                // GracePeriod command
                var gracePeriodRoot = Commands.literal("GracePeriod").requires(PERMISSIONS).build();
                var gracePeriodSeconds = Commands.argument("seconds", IntegerArgumentType.integer(0, 600)).executes(LockoutServer::setGracePeriod).build();

                dispatcher.getRoot().addChild(gracePeriodRoot);
                gracePeriodRoot.addChild(gracePeriodSeconds);
            }

            {
                // RemoveCustomBoard command (SetCustomBoard is registered in LockoutClient, and server listens for a packet)

                dispatcher.getRoot().addChild(Commands.literal("RemoveCustomBoard").requires(PERMISSIONS).executes((context) -> {
                    ClientPlayNetworking.send(new CustomBoardPayload(Optional.empty()));
                    return 1;
                }).build());
            }

            {
                // SetBoardSize command

                var setBoardTimeRoot = Commands.literal("SetBoardSize").requires(PERMISSIONS).build();
                var size = Commands.argument("board size", IntegerArgumentType.integer(MIN_BOARD_SIZE, MAX_BOARD_SIZE)).executes(LockoutServer::setBoardSize).build();

                dispatcher.getRoot().addChild(setBoardTimeRoot);
                setBoardTimeRoot.addChild(size);
            }

            {
                // RemovePicks command
                dispatcher.getRoot().addChild(Commands.literal("RemovePicks").requires(PERMISSIONS).executes((context) -> {
                    // Remove goal-to-player mappings for picks before clearing
                    for (String goalId : GoalGroup.PICKS.getGoals()) {
                        GoalGroup.setGoalPlayer(goalId, null);
                    }
                    GoalGroup.PICKS.getGoals().clear();
                    LockoutServer.SERVER_PICKS.clear();
                    context.getSource().sendSystemMessage(Component.literal("Removed picks."));
                    
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
                        for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                    return 1;
                }).build());
            }

            {
                // RemoveBans command
                dispatcher.getRoot().addChild(Commands.literal("RemoveBans").requires(PERMISSIONS).executes((context) -> {
                    // Remove goal-to-player mappings for bans before clearing
                    for (String goalId : GoalGroup.BANS.getGoals()) {
                        GoalGroup.setGoalPlayer(goalId, null);
                    }
                    GoalGroup.BANS.getGoals().clear();
                    LockoutServer.SERVER_BANS.clear();
                    context.getSource().sendSystemMessage(Component.literal("Removed bans."));
                    
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
                        for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                    return 1;
                }).build());
            }

            {
                // SimulatePickBans command
                var simulatePickBansRoot = Commands.literal("SimulatePickBans").requires(PERMISSIONS).build();
                var team1Arg = Commands.argument("team1", StringArgumentType.word()).suggests(new TeamSuggestionProvider()).build();
                var team2Arg = Commands.argument("team2", StringArgumentType.word()).suggests(new TeamSuggestionProvider()).executes(context -> {
                    String team1Name = StringArgumentType.getString(context, "team1");
                    String team2Name = StringArgumentType.getString(context, "team2");
                    
                    ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
                    Team team1 = scoreboard.getPlayerTeam(team1Name);
                    Team team2 = scoreboard.getPlayerTeam(team2Name);
                    
                    if (team1 == null) {
                        context.getSource().sendFailure(Component.literal("Team " + team1Name + " does not exist."));
                        return 0;
                    }
                    if (team2 == null) {
                        context.getSource().sendFailure(Component.literal("Team " + team2Name + " does not exist."));
                        return 0;
                    }
                    if (team1.equals(team2)) {
                        context.getSource().sendFailure(Component.literal("Cannot start pick/ban with the same team twice."));
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
                    Commands.literal("CancelPickBanSession")
                        .requires(PERMISSIONS)
                        .executes(context -> {
                            if (LockoutServer.activePickBanSession == null) {
                                context.getSource().sendFailure(Component.literal("No active pick/ban session to cancel."));
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
                            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                ServerPlayNetworking.send(player, payload);
                            }
                            
                            context.getSource().sendSystemMessage(Component.literal("Pick/ban session cancelled."));
                            return 1;
                        })
                        .build()
                );
            }

            {
                // PickBanSelectionLimit command
                dispatcher.getRoot().addChild(
                    Commands.literal("PickBanSelectionLimit")
                        .requires(PERMISSIONS)
                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 5))
                            .executes(context -> {
                                int limit = IntegerArgumentType.getInteger(context, "limit");
                                
                                if (LockoutServer.activePickBanSession == null) {
                                    // If no active session, store the limit for the next session
                                    LockoutServer.defaultPickBanLimit = limit;
                                    context.getSource().sendSystemMessage(Component.literal("Pick/ban selection limit set to " + limit + " for the next session."));
                                    return 1;
                                }
                                
                                // Can only change limit before any rounds are locked
                                if (LockoutServer.activePickBanSession.getCurrentRound() > 1) {
                                    context.getSource().sendFailure(Component.literal("Cannot change selection limit after round 1 has started."));
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
                                
                                for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                    ServerPlayNetworking.send(player, payload);
                                }
                                
                                context.getSource().sendSystemMessage(Component.literal("Pick/ban selection limit set to " + limit));
                                return 1;
                            })
                        )
                        .build()
                );
            }

            {
                // MaxRounds command
                dispatcher.getRoot().addChild(
                    Commands.literal("MaxRounds")
                        .requires(PERMISSIONS)
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(2, 10))
                            .executes(context -> {
                                int rounds = IntegerArgumentType.getInteger(context, "rounds");
                                
                                if (rounds % 2 != 0) {
                                    context.getSource().sendFailure(Component.literal("Max rounds must be an even number."));
                                    return 0;
                                }
                                
                                if (LockoutServer.activePickBanSession != null) {
                                    context.getSource().sendFailure(Component.literal("Cannot change max rounds during an active session."));
                                    return 0;
                                }
                                
                                LockoutServer.defaultMaxRounds = rounds;
                                context.getSource().sendSystemMessage(Component.literal("Max rounds set to " + rounds + " for the next session."));
                                return 1;
                            })
                        )
                        .build()
                );
            }

        });

        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            if (!key.equals(BuiltInLootTables.PIGLIN_BARTERING)) return null;

            var soulSpeed = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SOUL_SPEED);

            var pool = LootPool.lootPool()
                .add(LootItem.lootTableItem(Items.BOOK)
                    .apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(soulSpeed))
                    .setWeight(5))
                .add(LootItem.lootTableItem(Items.IRON_BOOTS)
                    .apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(soulSpeed))
                    .setWeight(8))
                .add(LootItem.lootTableItem(Items.POTION)
                    .apply(SetPotionFunction.setPotion(Potions.FIRE_RESISTANCE))
                    .setWeight(10))
                .add(LootItem.lootTableItem(Items.SPLASH_POTION)
                    .apply(SetPotionFunction.setPotion(Potions.FIRE_RESISTANCE))
                    .setWeight(10))
                .add(LootItem.lootTableItem(Items.IRON_NUGGET)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(9), ConstantValue.exactly(36))))
                    .setWeight(10))
                .add(LootItem.lootTableItem(Items.QUARTZ)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(8), ConstantValue.exactly(16))))
                    .setWeight(20))
                .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(5), ConstantValue.exactly(12))))
                    .setWeight(20))
                .add(LootItem.lootTableItem(Items.MAGMA_CREAM)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(2), ConstantValue.exactly(6))))
                    .setWeight(20))
                .add(LootItem.lootTableItem(Items.ENDER_PEARL)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(4), ConstantValue.exactly(8))))
                    .setWeight(20))
                .add(LootItem.lootTableItem(Items.STRING)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(8), ConstantValue.exactly(24))))
                    .setWeight(20))
                .add(LootItem.lootTableItem(Items.FIRE_CHARGE)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(1), ConstantValue.exactly(5))))
                    .setWeight(40))
                .add(LootItem.lootTableItem(Items.GRAVEL)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(8), ConstantValue.exactly(16))))
                    .setWeight(40))
                .add(LootItem.lootTableItem(Items.LEATHER)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(4), ConstantValue.exactly(10))))
                    .setWeight(40))
                .add(LootItem.lootTableItem(Items.NETHER_BRICK)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(4), ConstantValue.exactly(16))))
                    .setWeight(40))
                .add(LootItem.lootTableItem(Items.OBSIDIAN)
                    .setWeight(40))
                .add(LootItem.lootTableItem(Items.CRYING_OBSIDIAN)
                    .apply(SetItemCountFunction.setCount(new UniformGenerator(ConstantValue.exactly(1), ConstantValue.exactly(3))))
                    .setWeight(40));

            return LootTable.lootTable().withPool(pool).build();
        });

    }

}
