package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutRunnable;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.have_more.HaveMostCreeperKillsGoal;
import me.marin.lockout.lockout.goals.have_more.HaveMostXPLevelsGoal;
import me.marin.lockout.lockout.goals.misc.EmptyHungerBarGoal;
import me.marin.lockout.lockout.goals.misc.ReachBedrockGoal;
import me.marin.lockout.lockout.goals.misc.ReachHeightLimitGoal;
import me.marin.lockout.lockout.goals.misc.ReachNetherRoofGoal;
import me.marin.lockout.lockout.goals.opponent.OpponentTouchesWaterGoal;
import me.marin.lockout.lockout.goals.wear_armor.WearCarvedPumpkinFor5MinutesGoal;
import me.marin.lockout.lockout.interfaces.HaveEffectsAppliedForXMinutesGoal;
import me.marin.lockout.lockout.interfaces.ObtainItemsGoal;
import me.marin.lockout.lockout.interfaces.OpponentObtainsItemGoal;
import me.marin.lockout.lockout.interfaces.RideEntityGoal;
import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
// TODO: SaddledComponent changed - import net.minecraft.world.entity.SaddledComponent;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import static me.marin.lockout.server.LockoutServer.gameStartRunnables;
import static me.marin.lockout.server.LockoutServer.lockout;

public class EndServerTickEventHandler implements ServerTickEvents.EndTick {

    @Override
    public void onEndTick(MinecraftServer server) {
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (LockoutRunnable runnable : new HashSet<>(gameStartRunnables.keySet())) {
            if (gameStartRunnables.get(runnable) <= 0) {
                runnable.run();
                gameStartRunnables.remove(runnable);
            } else {
                gameStartRunnables.merge(runnable, -1L, Long::sum);
            }
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;

            if (goal instanceof HaveMostXPLevelsGoal) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    // Only track lockout players, not spectators
                    if (lockout.isLockoutPlayer(player.getUUID())) {
                        lockout.levels.put(player.getUUID(), player.isDeadOrDying() ? 0 : player.experienceLevel);
                    }
                }
                lockout.recalculateXPGoal(goal);
                // Send tooltip updates to all teams
                for (LockoutTeam team : lockout.getTeams()) {
                    ((LockoutTeamServer) team).sendTooltipUpdate((HaveMostXPLevelsGoal) goal, true);
                }
            }

            if (goal instanceof HaveMostCreeperKillsGoal) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    // Only track lockout players, not spectators
                    if (lockout.isLockoutPlayer(player.getUUID())) {
                        int creeperKills = player.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.CREEPER));
                        lockout.creeperKills.put(player.getUUID(), creeperKills);
                    }
                }
                lockout.recalculateCreeperKillsGoal(goal);
                // Send tooltip updates to all teams
                for (LockoutTeam team : lockout.getTeams()) {
                    ((LockoutTeamServer) team).sendTooltipUpdate((HaveMostCreeperKillsGoal) goal, true);
                }
            }

            if (goal.isCompleted()) continue;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (goal instanceof ObtainItemsGoal obtainItemsGoal && !(goal instanceof WearCarvedPumpkinFor5MinutesGoal)) {
                    if (obtainItemsGoal.satisfiedBy(player.getInventory())) {
                        if (goal instanceof OpponentObtainsItemGoal opponentObtainsItemGoal) {
                            lockout.complete1v1Goal(goal, player, false, opponentObtainsItemGoal.getMessage(player));
                        } else {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }

                if (goal instanceof RideEntityGoal rideEntityGoal && player.isPassenger()) {
                    EntityType<?> vehicle = player.getVehicle().getType();

                    if (Objects.equals(vehicle, rideEntityGoal.getEntityType()) || (rideEntityGoal.getEntityType() == EntityTypes.NAUTILUS && vehicle == EntityTypes.ZOMBIE_NAUTILUS)) {
                        boolean allow = true;
                        if (Objects.equals(vehicle, EntityTypes.PIG)) {
                            boolean hasCarrotOnAStick = false;
                            var handItem = player.getInventory().getSelectedItem();
                            if (handItem.getItem().equals(Items.CARROT_ON_A_STICK)) {
                                hasCarrotOnAStick = true;
                            }
                            allow = hasCarrotOnAStick;
                        }
                        if (player.getVehicle() instanceof AbstractHorse horse) {
                            allow = false;
                            allow = horse.isTamed() && player.equals(horse.getControllingPassenger());
                        }
                        if (allow) {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }
                if (goal instanceof EmptyHungerBarGoal) {
                    if (player.getFoodData().getFoodLevel() == 0) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachHeightLimitGoal) {
                    if (player.getY() >= 320 && player.level().dimension().equals(Level.OVERWORLD)) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachNetherRoofGoal) {
                    if (player.getY() >= 128 && player.level().dimension().equals(Level.NETHER)) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachBedrockGoal) {
                    if (player.getY() < 10 && Objects.equals(player.level().getBlockState(player.blockPosition().below()).getBlock(), Blocks.BEDROCK)) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof OpponentTouchesWaterGoal) {
                    if (Objects.equals(player.level().getBlockState(player.blockPosition()).getBlock(), Blocks.WATER)) {
                        lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " touched water.");
                    }
                }
            }

            // Handle HaveEffectsAppliedForXMinutesGoal once per team
            if (goal instanceof HaveEffectsAppliedForXMinutesGoal haveEffectsGoal) {
                for (LockoutTeamServer team : lockout.getTeams().stream()
                        .filter(t -> t instanceof LockoutTeamServer)
                        .map(t -> (LockoutTeamServer) t)
                        .toList()) {
                    
                    // Track individual player times and calculate team total
                    long totalTeamTime = 0;
                    boolean anyProgress = false;
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        UUID uuid = player.getUUID();
                        if (team.getPlayers().contains(uuid)) {
                            var map = lockout.appliedEffectsTime;
                            long appliedTime = map.getOrDefault(uuid, 0L);
                            
                            // Only increment if THIS player has status effects
                            if (!player.getActiveEffects().isEmpty()) {
                                appliedTime++;
                                map.put(uuid, appliedTime);
                                anyProgress = true;
                            }
                            
                            totalTeamTime += appliedTime;
                        }
                    }

                    // Complete goal when team's total time reaches threshold
                    if (totalTeamTime >= (20 * 60 * haveEffectsGoal.getMinutes())) {
                        lockout.completeGoal(goal, team);
                    }

                    // Send tooltip update whenever progress is made
                    if (anyProgress) {
                        team.sendTooltipUpdate(haveEffectsGoal, true);
                    }
                }
            }

            // Handle WearCarvedPumpkinFor5MinutesGoal once per team
            if (goal instanceof WearCarvedPumpkinFor5MinutesGoal pumpkinGoal) {
                for (LockoutTeamServer team : lockout.getTeams().stream()
                        .filter(t -> t instanceof LockoutTeamServer)
                        .map(t -> (LockoutTeamServer) t)
                        .toList()) {
                    
                    // Track individual player times and calculate team total
                    long totalTeamTime = 0;
                    boolean anyProgress = false;
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        UUID uuid = player.getUUID();
                        if (team.getPlayers().contains(uuid)) {
                            var map = lockout.pumpkinWearTime;
                            long wornTime = map.getOrDefault(uuid, 0L);
                            
                            // Only increment if THIS player is wearing a pumpkin
                            ItemStack helmet = ((PlayerInventoryAccessor) player.getInventory()).getEquipment().get(EquipmentSlot.HEAD);
                            if (helmet != null && helmet.getItem() == Items.CARVED_PUMPKIN) {
                                wornTime++;
                                map.put(uuid, wornTime);
                                anyProgress = true;
                            }
                            
                            totalTeamTime += wornTime;
                        }
                    }

                    // Complete goal when team's total time reaches threshold
                    if (totalTeamTime >= (20 * 60 * 5)) {
                        lockout.completeGoal(goal, team);
                    }

                    // Send tooltip update whenever progress is made
                    if (anyProgress) {
                        team.sendTooltipUpdate(pumpkinGoal, true);
                    }
                }
            }
        }

        lockout.tick();
        
        // Check if grace period just ended
        int gracePeriod = me.marin.lockout.server.LockoutServer.getGracePeriodSeconds();
        if (gracePeriod > 0 && lockout.getTicks() == 20L * gracePeriod) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal("Grace period over! PvP enabled.").withStyle(ChatFormatting.RED));
            }
        }
        
        // Check for expired spawn protection every second
        if (lockout.getTicks() % 20 == 0) {
            long gracePeriodTicks = 20L * 30; // 30 seconds
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (lockout.isLockoutPlayer(player.getUUID())) {
                    Long deathTime = me.marin.lockout.server.LockoutServer.playerDeathTimes.get(player.getUUID());
                    if (deathTime != null && lockout.getTicks() - deathTime == gracePeriodTicks) {
                        player.sendSystemMessage(Component.literal("Your spawn protection has expired! PvP enabled.").withStyle(ChatFormatting.GOLD));
                        me.marin.lockout.server.LockoutServer.playerDeathTimes.remove(player.getUUID());
                    }
                }
            }
        }
        
        if (lockout.getTicks() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());
            }
        }
    }
}
