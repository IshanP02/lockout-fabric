package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.death.DieToFallingOffVinesGoal;
import me.marin.lockout.lockout.goals.death.DieToTNTMinecartGoal;
import me.marin.lockout.lockout.goals.kill.*;
import me.marin.lockout.lockout.goals.opponent.OpponentDies3TimesGoal;
import me.marin.lockout.lockout.goals.opponent.OpponentDiesGoal;
import me.marin.lockout.lockout.interfaces.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.FallLocation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static me.marin.lockout.server.LockoutServer.lockout;

public class AfterDeathEventHandler implements ServerLivingEntityEvents.AfterDeath {
    @Override
    public void afterDeath(LivingEntity entity, DamageSource source) {
        if (!Lockout.isLockoutRunning(lockout)) {
            return;
        }
        if (entity instanceof Player player && !lockout.isLockoutPlayer(player)) {
            return;
        }

        boolean playerDied = entity instanceof Player;
        boolean mobDied = !playerDied;
        boolean killedByPlayer = entity.getKillCredit() instanceof Player;

        if (playerDied) {
            LockoutTeam team = lockout.getPlayerTeam(entity.getUUID());

            lockout.deaths.putIfAbsent(team, 0);
            lockout.deaths.merge(team, 1, Integer::sum);
            
            // Track this death for statistics - only mark as task death if it actually completes a death goal
            boolean isTaskDeath = false;
            
            // Check if this death actually completes a death-related goal
            for (Goal goal : lockout.getBoard().getGoals()) {
                if (goal == null || goal.isCompleted()) continue;
                
                // Check if this specific death matches and would complete a death goal
                if (goal instanceof DieToDamageTypeGoal dieToDamageTypeGoal) {
                    for (ResourceKey<DamageType> key : dieToDamageTypeGoal.getDamageRegistryKeys()) {
                        if (source.typeHolder().is(key)) {
                            isTaskDeath = true;
                            break;
                        }
                    }
                } else if (goal instanceof DieToEntityGoal dieToEntityGoal) {
                    if (source.getEntity() != null && source.getEntity().getType() == dieToEntityGoal.getEntityType()) {
                        isTaskDeath = true;
                    }
                } else if (goal instanceof DieToFallingOffVinesGoal) {
                    if (source.typeHolder().is(DamageTypes.FALL)) {
                        FallLocation fallLocation = FallLocation.getCurrentFallLocation((Player) entity);
                        if (fallLocation != null && List.of(FallLocation.VINES, FallLocation.TWISTING_VINES, FallLocation.WEEPING_VINES).contains(fallLocation)) {
                            isTaskDeath = true;
                        }
                    }
                } else if (goal instanceof DieToTNTMinecartGoal) {
                    if (source.getDirectEntity() instanceof MinecartTNT) {
                        isTaskDeath = true;
                    }
                }
                
                if (isTaskDeath) break;
            }
            
            // Record death in statistics
            if (lockout.getStatistics() != null) {
                lockout.getStatistics().recordPlayerDeath(entity.getUUID(), isTaskDeath);
            }
        }
        if (mobDied && killedByPlayer) {
            Player killer = (Player) entity.getKillCredit();
            if (lockout.isLockoutPlayer(killer.getUUID())) {
                LockoutTeam team = lockout.getPlayerTeam(killer.getUUID());
                lockout.mobsKilled.putIfAbsent(team, 0);
                lockout.mobsKilled.merge(team, 1, Integer::sum);
                
                // Track per-player for statistics
                lockout.playerMobsKilled.putIfAbsent(killer.getUUID(), 0);
                lockout.playerMobsKilled.merge(killer.getUUID(), 1, Integer::sum);
            }
        }
        
        // Track player kills for statistics (only PvP between different teams)
        if (playerDied && killedByPlayer) {
            Player killer = (Player) entity.getKillCredit();
            Player victim = (Player) entity;
            if (lockout.isLockoutPlayer(killer.getUUID()) && lockout.isLockoutPlayer(victim.getUUID())) {
                // Only count as player kill if they're on different teams
                LockoutTeam killerTeam = lockout.getPlayerTeam(killer.getUUID());
                LockoutTeam victimTeam = lockout.getPlayerTeam(victim.getUUID());
                if (!Objects.equals(killerTeam, victimTeam)) {
                    if (lockout.getStatistics() != null) {
                        lockout.getStatistics().recordPlayerKill(killer.getUUID());
                    }
                }
            }
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (mobDied && killedByPlayer) {
                Player killer = (Player) entity.getKillCredit();

                if (goal instanceof KillMobGoal killMobGoal) {
                    if (killMobGoal.getEntity().equals(entity.getType())) {
                        boolean allow = true;
                        if (goal instanceof KillSnowGolemInNetherGoal)  {
                            allow = killer.level().dimension().equals(Level.NETHER);
                        }
                        if (goal instanceof KillBreezeWithWindChargeGoal) {
                            allow = source.is(DamageTypes.WIND_CHARGE);
                        }
                        if (goal instanceof KillBlazeWithSnowballGoal) {
                            allow = source.is(DamageTypes.THROWN);
                        }
                        if (goal instanceof KillColoredSheepGoal killColoredSheepGoal) {
                            allow = ((Sheep) entity).getColor() == killColoredSheepGoal.getDyeColor();
                        }
                        if (allow) {
                            lockout.completeGoal(goal, killer);
                        }
                    }
                }
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(killer.getUUID());

                if (goal instanceof KillAllSpecificMobsGoal killAllSpecificMobsGoal) {
                    if (killAllSpecificMobsGoal.getEntityTypes().contains(entity.getType())) {
                        killAllSpecificMobsGoal.getTrackerMap().computeIfAbsent(team, t -> new LinkedHashSet<>());
                        killAllSpecificMobsGoal.getTrackerMap().get(team).add(entity.getType());

                        int size = killAllSpecificMobsGoal.getTrackerMap().get(team).size();

                        team.sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                        if (size >= killAllSpecificMobsGoal.getEntityTypes().size()) {
                            lockout.completeGoal(killAllSpecificMobsGoal, team);
                        }
                    }
                }
                if (goal instanceof KillUniqueHostileMobsGoal killUniqueHostileMobsGoal) {
                    if (entity instanceof Monster) {
                        lockout.killedHostileTypes.computeIfAbsent(team, t -> new LinkedHashSet<>());
                        boolean newHostile = lockout.killedHostileTypes.get(team).add(entity.getType());
                        
                        // Track per-player for statistics
                        lockout.playerKilledHostileMobs.computeIfAbsent(killer.getUUID(), p -> new LinkedHashSet<>());
                        lockout.playerKilledHostileMobs.get(killer.getUUID()).add(entity.getType());
                        
                        // Track first contributor
                        if (newHostile) {
                            lockout.firstHostileKillContributor.putIfAbsent(team, new HashMap<>());
                            lockout.firstHostileKillContributor.get(team).put(entity.getType(), killer.getUUID());
                        }

                        int size = lockout.killedHostileTypes.get(team).size();

                        team.sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                        if (size >= killUniqueHostileMobsGoal.getAmount()) {
                            lockout.completeGoal(killUniqueHostileMobsGoal, team);
                        }
                    }
                }
                if (goal instanceof Kill100MobsGoal kill100MobsGoal) {
                    int size = lockout.mobsKilled.get(team);

                    team.sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                    if (size >= kill100MobsGoal.getAmount()) {
                        lockout.completeGoal(goal, team);
                    }
                }
                if (goal instanceof KillSpecificMobsGoal killSpecificMobsGoal) {
                    if (killSpecificMobsGoal.getEntityTypes().contains(entity.getType())) {
                        killSpecificMobsGoal.getTrackerMap().computeIfAbsent(team, t -> 0);
                        killSpecificMobsGoal.getTrackerMap().merge(team, 1, Integer::sum);
                        
                        // Track per-player for statistics
                        if (killSpecificMobsGoal.getTrackerMap() == lockout.killedArthropods) {
                            lockout.playerKilledArthropods.putIfAbsent(killer.getUUID(), 0);
                            lockout.playerKilledArthropods.merge(killer.getUUID(), 1, Integer::sum);
                        } else if (killSpecificMobsGoal.getTrackerMap() == lockout.killedUndeadMobs) {
                            lockout.playerKilledUndeadMobs.putIfAbsent(killer.getUUID(), 0);
                            lockout.playerKilledUndeadMobs.merge(killer.getUUID(), 1, Integer::sum);
                        }

                        int size = killSpecificMobsGoal.getTrackerMap().get(team);

                        team.sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                        if (size >= killSpecificMobsGoal.getAmount()) {
                            lockout.completeGoal(killSpecificMobsGoal, killer);
                        }
                    }
                }
            }
            if (playerDied) {
                Player player = (Player) entity;
                LockoutTeam team = lockout.getPlayerTeam(player.getUUID());

                if (goal instanceof OpponentDiesGoal) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " died.");
                }
                if (goal instanceof OpponentDies3TimesGoal && lockout.deaths.get(team) >= 3) {
                    lockout.complete1v1Goal(goal, player, false, team.getDisplayName() + " died 3 times.");
                }
                if (goal instanceof DieToDamageTypeGoal dieToDamageTypeGoal) {
                    for (ResourceKey<DamageType> key : dieToDamageTypeGoal.getDamageRegistryKeys()) {
                        if (source.typeHolder().is(key)) {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }
                if (goal instanceof DieToEntityGoal dieToEntityGoal) {
                    if (source.getEntity() != null && source.getEntity().getType() == dieToEntityGoal.getEntityType()) {
                        lockout.completeGoal(goal, player);
                    }
                }
                if (goal instanceof DieToFallingOffVinesGoal) {
                    if (source.typeHolder().is(DamageTypes.FALL)) {
                        FallLocation fallLocation = FallLocation.getCurrentFallLocation(player);
                        if (fallLocation != null) {
                            if (List.of(FallLocation.VINES, FallLocation.TWISTING_VINES, FallLocation.WEEPING_VINES).contains(fallLocation)) {
                                lockout.completeGoal(goal, player);
                            }
                        }
                    }
                }
                if (goal instanceof DieToTNTMinecartGoal) {
                    if (source.getDirectEntity() instanceof MinecartTNT) {
                        lockout.completeGoal(goal, player);
                    }
                }

                if (goal instanceof KillOtherTeamPlayer && killedByPlayer) {
                    Player killer = (Player) entity.getKillCredit();

                    if (!Objects.equals(player, killer) && !Objects.equals(lockout.getPlayerTeam(killer.getUUID()), lockout.getPlayerTeam(player.getUUID()))) {
                        lockout.completeGoal(goal, killer);
                    }
                }
            }


        }

    }
}
