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
import net.minecraft.entity.EquipmentSlot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SaddledComponent;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    lockout.levels.put(player.getUuid(), player.isDead() ? 0 : player.experienceLevel);
                }
                lockout.recalculateXPGoal(goal);
                // Send tooltip updates to all teams
                for (LockoutTeam team : lockout.getTeams()) {
                    ((LockoutTeamServer) team).sendTooltipUpdate((HaveMostXPLevelsGoal) goal, true);
                }
            }

            if (goal instanceof HaveMostCreeperKillsGoal) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    int creeperKills = player.getStatHandler().getStat(net.minecraft.stat.Stats.KILLED.getOrCreateStat(net.minecraft.entity.EntityType.CREEPER));
                    lockout.creeperKills.put(player.getUuid(), creeperKills);
                }
                lockout.recalculateCreeperKillsGoal(goal);
                // Send tooltip updates to all teams
                for (LockoutTeam team : lockout.getTeams()) {
                    ((LockoutTeamServer) team).sendTooltipUpdate((HaveMostCreeperKillsGoal) goal, true);
                }
            }

            if (goal.isCompleted()) continue;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (goal instanceof ObtainItemsGoal obtainItemsGoal && !(goal instanceof WearCarvedPumpkinFor5MinutesGoal)) {
                    if (obtainItemsGoal.satisfiedBy(player.getInventory())) {
                        if (goal instanceof OpponentObtainsItemGoal opponentObtainsItemGoal) {
                            lockout.complete1v1Goal(goal, player, false, opponentObtainsItemGoal.getMessage(player));
                        } else {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }

                if (goal instanceof RideEntityGoal rideEntityGoal && player.hasVehicle()) {
                    EntityType<?> vehicle = player.getVehicle().getType();

                    if (Objects.equals(vehicle, rideEntityGoal.getEntityType()) || (rideEntityGoal.getEntityType() == EntityType.NAUTILUS && vehicle == EntityType.ZOMBIE_NAUTILUS)) {
                        boolean allow = true;
                        if (Objects.equals(vehicle, EntityType.PIG)) {
                            boolean hasCarrotOnAStick = false;
                            var handItem = player.getInventory().getSelectedStack();
                            if (handItem.getItem().equals(Items.CARROT_ON_A_STICK)) {
                                hasCarrotOnAStick = true;
                            }
                            allow = hasCarrotOnAStick;
                        }
                        if (player.getVehicle() instanceof AbstractHorseEntity horse) {
                            allow = false;
                            allow = horse.isTame() && horse.isControlledByPlayer();
                        }
                        if (allow) {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }
                if (goal instanceof EmptyHungerBarGoal) {
                    if (player.getHungerManager().getFoodLevel() == 0) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachHeightLimitGoal) {
                    if (player.getY() >= 320 && player.getEntityWorld().getRegistryKey() == ServerWorld.OVERWORLD) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachNetherRoofGoal) {
                    if (player.getY() >= 128 && player.getEntityWorld().getRegistryKey() == ServerWorld.NETHER) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof ReachBedrockGoal) {
                    if (player.getY() < 10 && Objects.equals(player.getEntityWorld().getBlockState(player.getBlockPos().down()).getBlock(), Blocks.BEDROCK)) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof OpponentTouchesWaterGoal) {
                    if (Objects.equals(player.getEntityWorld().getBlockState(player.getBlockPos()).getBlock(), Blocks.WATER)) {
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
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        UUID uuid = player.getUuid();
                        if (team.getPlayers().contains(uuid)) {
                            var map = lockout.appliedEffectsTime;
                            long appliedTime = map.getOrDefault(uuid, 0L);
                            
                            // Only increment if THIS player has status effects
                            if (!player.getStatusEffects().isEmpty()) {
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
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        UUID uuid = player.getUuid();
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
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(Text.literal("Grace period over! PvP enabled.").formatted(Formatting.RED), false);
            }
        }
        
        // Check for expired spawn protection every second
        if (lockout.getTicks() % 20 == 0) {
            long gracePeriodTicks = 20L * 30; // 30 seconds
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (lockout.isLockoutPlayer(player.getUuid())) {
                    Long deathTime = me.marin.lockout.server.LockoutServer.playerDeathTimes.get(player.getUuid());
                    if (deathTime != null && lockout.getTicks() - deathTime == gracePeriodTicks) {
                        player.sendMessage(Text.literal("Your spawn protection has expired! PvP enabled.").formatted(Formatting.GOLD), false);
                        me.marin.lockout.server.LockoutServer.playerDeathTimes.remove(player.getUuid());
                    }
                }
            }
        }
        
        if (lockout.getTicks() % 20 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, lockout.getUpdateTimerPacket());
            }
        }
    }
}
