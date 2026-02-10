package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.HaveShieldDisabledGoal;
import me.marin.lockout.lockout.goals.misc.PlacePaintingGoal;
import me.marin.lockout.lockout.goals.misc.RightClickBannerWithMapGoal;
import me.marin.lockout.lockout.goals.misc.BreakAnyPieceOfArmorGoal;
import me.marin.lockout.lockout.goals.misc.Sprint1KmGoal;
import me.marin.lockout.lockout.goals.misc.Boat2KmGoal;
import me.marin.lockout.lockout.goals.misc.BreakAnyToolGoal;
import me.marin.lockout.lockout.goals.misc.Take200DamageGoal;
import me.marin.lockout.lockout.goals.opponent.*;
import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import me.marin.lockout.lockout.interfaces.ReachXPLevelGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.marin.lockout.lockout.goals.misc.Crouch100mGoal;
import me.marin.lockout.lockout.goals.misc.Swim500mGoal;
import me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {

    @Inject(method = "collideWithEntity", at = @At("HEAD"))
    public void onCollide(Entity entity, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof OpponentHitBySnowballGoal) {
                if (entity instanceof SnowballEntity snowballEntity) {
                    if (snowballEntity.getOwner() instanceof PlayerEntity shooter && !Objects.equals(player, shooter)) {
                        lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with a Snowball.");
                    }
                }
            }
            if (goal instanceof OpponentHitByEggGoal) {
                if (entity instanceof EggEntity eggEntity) {
                    if (eggEntity.getOwner() instanceof PlayerEntity shooter && !Objects.equals(player, shooter)) {
                        lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with an Egg.");
                    }
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void onStartMatch(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (world.isClient()) return;
        if (!lockout.hasStarted()) {
            cir.setReturnValue(false);
            return;
        }
        
        // Grace period: prevent PVP damage from other players
        int gracePeriod = LockoutServer.getGracePeriodSeconds();
        if (gracePeriod > 0 && lockout.getTicks() < 20L * gracePeriod) {
            if (source.getAttacker() instanceof PlayerEntity attacker) {
                PlayerEntity victim = (PlayerEntity) (Object) this;
                if (lockout.isLockoutPlayer(victim.getUuid()) && lockout.isLockoutPlayer(attacker.getUuid())) {
                    // Both are lockout players, check if they're on different teams
                    if (!lockout.getPlayerTeam(victim.getUuid()).equals(lockout.getPlayerTeam(attacker.getUuid()))) {
                        long remainingSeconds = (20L * gracePeriod - lockout.getTicks()) / 20;
                        if (attacker instanceof ServerPlayerEntity serverAttacker) {
                            serverAttacker.sendMessage(Text.literal(remainingSeconds + " seconds until grace period ends!").formatted(Formatting.RED), false);
                        }
                        cir.setReturnValue(false);
                    }
                }
            }
        }
        
        // Death-based grace period: 30 seconds of PVP immunity after respawn
        if (source.getAttacker() instanceof PlayerEntity attacker) {
            PlayerEntity victim = (PlayerEntity) (Object) this;
            if (lockout.isLockoutPlayer(victim.getUuid()) && lockout.isLockoutPlayer(attacker.getUuid())) {
                // Check if they're on different teams
                if (!lockout.getPlayerTeam(victim.getUuid()).equals(lockout.getPlayerTeam(attacker.getUuid()))) {
                    long gracePeriodTicks = 20L * 30; // 30 seconds
                    
                    // Check if victim is in grace period
                    Long victimDeathTime = LockoutServer.playerDeathTimes.get(victim.getUuid());
                    if (victimDeathTime != null && lockout.getTicks() - victimDeathTime < gracePeriodTicks) {
                        long remainingSeconds = (gracePeriodTicks - (lockout.getTicks() - victimDeathTime)) / 20;
                        if (attacker instanceof ServerPlayerEntity serverAttacker) {
                            serverAttacker.sendMessage(Text.literal(victim.getName().getString() + " has " + remainingSeconds + " seconds of spawn protection!").formatted(Formatting.RED), false);
                        }
                        cir.setReturnValue(false);
                        return;
                    }
                    
                    // Check if attacker is in grace period (also can't attack)
                    Long attackerDeathTime = LockoutServer.playerDeathTimes.get(attacker.getUuid());
                    if (attackerDeathTime != null && lockout.getTicks() - attackerDeathTime < gracePeriodTicks) {
                        long remainingSeconds = (gracePeriodTicks - (lockout.getTicks() - attackerDeathTime)) / 20;
                        if (attacker instanceof ServerPlayerEntity serverAttacker) {
                            serverAttacker.sendMessage(Text.literal("You have " + remainingSeconds + " seconds of spawn protection! You cannot attack other players.").formatted(Formatting.RED), false);
                        }
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    public void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!cir.getReturnValue()) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        if (!lockout.isLockoutPlayer(player.getUuid())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());

        lockout.damageTaken.putIfAbsent(team, 0d);
        lockout.damageTaken.merge(team, (double)amount, Double::sum);

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof Take200DamageGoal take200DamageGoal) {
                team.sendTooltipUpdate(take200DamageGoal);
                if (lockout.damageTaken.get(team) >= 200) {
                    lockout.completeGoal(goal, team);
                }
            }
            if (goal instanceof OpponentHitByArrowGoal) {
                if (source.isOf(DamageTypes.ARROW)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " got hit by Arrow.");
                }
            }
            if (goal instanceof OpponentHitByWindChargeGoal) {
                if (source.isOf(DamageTypes.WIND_CHARGE)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " got hit by Wind Charge.");
                }
            }
            if (goal instanceof OpponentTakesFallDamageGoal) {
                if (source.isOf(DamageTypes.FALL)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " took fall damage.");
                }
            }
            if (goal instanceof OpponentTakes100DamageGoal) {
                if (lockout.damageTaken.get(team) >= 100) {
                    lockout.complete1v1Goal(goal, team, false, team.getDisplayName() + " took 100 damage.");
                }
            }

            if (goal instanceof DamagedByUniqueSourcesGoal damagedGoal) {
                var entry = source.getTypeRegistryEntry();
                net.minecraft.registry.RegistryKey<net.minecraft.entity.damage.DamageType> damageTypeKey = null;
                if (entry != null) {
                    damageTypeKey = entry.getKey().orElse(null);
                }

                if (damageTypeKey != null) {
                    lockout.damageTypesTaken.computeIfAbsent(team, t -> new java.util.LinkedHashSet<>());

                    boolean added = lockout.damageTypesTaken.get(team).add(damageTypeKey);
                    if (added) {
                        lockout.damageByUniqueSources.putIfAbsent(team, 0);
                        lockout.damageByUniqueSources.merge(team, 1, Integer::sum);
                    }
                    
                    // Send tooltip update for this goal (whether damage was newly added or not)
                    if (team instanceof me.marin.lockout.LockoutTeamServer) {
                        team.sendTooltipUpdate(damagedGoal);
                    }
                }
                // Check for completion using dynamic amount
                int requiredAmount = damagedGoal.getAmount();
                if (lockout.damageByUniqueSources.get(team) >= requiredAmount) {
                    lockout.completeGoal(damagedGoal, team);
                }
            }

        }
    }

    @Inject(method = "incrementStat(Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    public void onIncrementStat(Identifier stat, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof IncrementStatGoal incrementStatGoal && incrementStatGoal.getStats().contains(stat)) {
                lockout.completeGoal(goal, player);
            }
            if (goal instanceof OpponentEatsFoodGoal && stat.equals(Stats.EAT_CAKE_SLICE)) {
                lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " ate food.");
            }
        }
    }

    @Inject(method = "increaseStat(Lnet/minecraft/util/Identifier;I)V", at = @At("HEAD"))
    public void onIncreaseStat(Identifier stat, int amount, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (goal instanceof Sprint1KmGoal && stat.equals(Stats.SPRINT_ONE_CM)) {
                lockout.distanceSprinted.putIfAbsent(player.getUuid(), 0);
                lockout.distanceSprinted.merge(player.getUuid(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUuid())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                    team.sendTooltipUpdate((Sprint1KmGoal) goal);
                }

                if (lockout.distanceSprinted.get(player.getUuid()) >= (100 * 1000)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Crouch100mGoal && stat.equals(Stats.CROUCH_ONE_CM)) {
                lockout.distanceCrouched.putIfAbsent(player.getUuid(), 0);
                lockout.distanceCrouched.merge(player.getUuid(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUuid())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                    team.sendTooltipUpdate((Crouch100mGoal) goal);
                }

                if (lockout.distanceCrouched.get(player.getUuid()) >= (100 * 100)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Swim500mGoal && stat.equals(Stats.SWIM_ONE_CM)) {
                lockout.distanceSwam.putIfAbsent(player.getUuid(), 0);
                lockout.distanceSwam.merge(player.getUuid(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUuid())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                    team.sendTooltipUpdate((Swim500mGoal) goal);
                }

                if (lockout.distanceSwam.get(player.getUuid()) >= (100 * 500)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Boat2KmGoal && stat.equals(Stats.BOAT_ONE_CM)) {
                lockout.distanceByBoat.putIfAbsent(player.getUuid(), 0);
                lockout.distanceByBoat.merge(player.getUuid(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUuid())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                    team.sendTooltipUpdate((Boat2KmGoal) goal);
                }

                if (lockout.distanceByBoat.get(player.getUuid()) >= (100 * 2000)) {
                    lockout.completeGoal(goal, player);
                }
            }
            
        }
    }

    @Inject(method = "addExperienceLevels", at = @At("TAIL"))
    public void onExperienceLevelUp(int levels, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ReachXPLevelGoal reachXPLevelGoal) {
                if (player.experienceLevel >= reachXPLevelGoal.getAmount()) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

    @Inject(method = "takeShieldHit", at = @At(value = "TAIL"))
    public void onTakeShieldHit(ServerWorld world, LivingEntity attacker, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;

        float f = attacker.getWeaponDisableBlockingForSeconds();

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (f <= 0.0F) continue;

            if (goal instanceof HaveShieldDisabledGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

    @Inject(method = "incrementStat(Lnet/minecraft/stat/Stat;)V", at = @At("HEAD"))
    private void onIncrementStat(Stat<?> stat, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getEntityWorld().isClient()) return;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        int currentValue = serverPlayer.getStatHandler().getStat(stat);

        if (currentValue == 0) {
            if (stat.getType() == Stats.USED) {
                @SuppressWarnings("unchecked")
                Stat<Item> itemStat = (Stat<Item>) stat;
                Item usedItem = itemStat.getValue();

                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal == null) continue;
                    if (goal.isCompleted()) continue;

                    if (goal instanceof PlacePaintingGoal && usedItem == Items.PAINTING) {
                        lockout.completeGoal(goal, player);
                    }
                    if (goal instanceof RightClickBannerWithMapGoal && usedItem == Items.FILLED_MAP) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }  
            if (stat.getType() == Stats.BROKEN) {
                @SuppressWarnings("unchecked")
                Stat<Item> itemStat = (Stat<Item>) stat;
                Item brokenItem = itemStat.getValue();

                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal == null) continue;
                    if (goal.isCompleted()) continue;
                    if (goal instanceof BreakAnyToolGoal) {
                        for(Item item : ((BreakAnyToolGoal) goal).getItemsToDisplay()) {
                            if (brokenItem == item) {
                                lockout.completeGoal(goal, player);
                            }
                        }
                    }
                    if (goal instanceof BreakAnyPieceOfArmorGoal) {
                        for(Item item : ((BreakAnyPieceOfArmorGoal) goal).getItemsToDisplay()) {
                            if (brokenItem == item) {
                                lockout.completeGoal(goal, player);
                            }
                        }
                    }
                    
                }
            }  
        }
    }

}
