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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.marin.lockout.lockout.goals.misc.Crouch100mGoal;
import me.marin.lockout.lockout.goals.misc.Swim500mGoal;
import me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal;

import java.util.LinkedHashSet;
import java.util.Objects;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void onStartMatch(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (world.isClientSide()) return;
        if (!lockout.hasStarted()) {
            cir.setReturnValue(false);
            return;
        }
        
        // Grace period: prevent PVP damage from other players
        int gracePeriod = LockoutServer.getGracePeriodSeconds();
        if (gracePeriod > 0 && lockout.getTicks() < 20L * gracePeriod) {
            if (source.getEntity() instanceof Player attacker) {
                Player victim = (Player) (Object) this;
                if (lockout.isLockoutPlayer(victim.getUUID()) && lockout.isLockoutPlayer(attacker.getUUID())) {
                    // Both are lockout players, check if they're on different teams
                    if (!lockout.getPlayerTeam(victim.getUUID()).equals(lockout.getPlayerTeam(attacker.getUUID()))) {
                        long remainingSeconds = (20L * gracePeriod - lockout.getTicks()) / 20;
                        if (attacker instanceof ServerPlayer serverAttacker) {
                            serverAttacker.sendSystemMessage(Component.literal(remainingSeconds + " seconds until grace period ends!").withStyle(ChatFormatting.RED));
                        }
                        cir.setReturnValue(false);
                    }
                }
            }
        }
        
        // Death-based grace period: 30 seconds of PVP immunity after respawn
        if (source.getEntity() instanceof Player attacker) {
            Player victim = (Player) (Object) this;
            if (lockout.isLockoutPlayer(victim.getUUID()) && lockout.isLockoutPlayer(attacker.getUUID())) {
                // Check if they're on different teams
                if (!lockout.getPlayerTeam(victim.getUUID()).equals(lockout.getPlayerTeam(attacker.getUUID()))) {
                    long gracePeriodTicks = 20L * 30; // 30 seconds
                    
                    // Check if victim is in grace period
                    Long victimDeathTime = LockoutServer.playerDeathTimes.get(victim.getUUID());
                    if (victimDeathTime != null && lockout.getTicks() - victimDeathTime < gracePeriodTicks) {
                        long remainingSeconds = (gracePeriodTicks - (lockout.getTicks() - victimDeathTime)) / 20;
                        if (attacker instanceof ServerPlayer serverAttacker) {
                            serverAttacker.sendSystemMessage(Component.literal(victim.getName().getString() + " has " + remainingSeconds + " seconds of spawn protection!").withStyle(ChatFormatting.RED));
                        }
                        cir.setReturnValue(false);
                        return;
                    }
                    
                    // Check if attacker is in grace period (also can't attack)
                    Long attackerDeathTime = LockoutServer.playerDeathTimes.get(attacker.getUUID());
                    if (attackerDeathTime != null && lockout.getTicks() - attackerDeathTime < gracePeriodTicks) {
                        long remainingSeconds = (gracePeriodTicks - (lockout.getTicks() - attackerDeathTime)) / 20;
                        if (attacker instanceof ServerPlayer serverAttacker) {
                            serverAttacker.sendSystemMessage(Component.literal("You have " + remainingSeconds + " seconds of spawn protection! You cannot attack other players.").withStyle(ChatFormatting.RED));
                        }
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    public void onDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!cir.getReturnValue()) return;

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        if (!lockout.isLockoutPlayer(player.getUUID())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());

        lockout.damageTaken.putIfAbsent(team, 0d);
        lockout.damageTaken.merge(team, (double)amount, Double::sum);
        
        // Track per-player for statistics
        lockout.playerDamageTaken.putIfAbsent(player.getUUID(), 0d);
        lockout.playerDamageTaken.merge(player.getUUID(), (double)amount, Double::sum);
        
        // Track damage types per-player for statistics
        lockout.playerDamageTypesTaken.computeIfAbsent(player.getUUID(), p -> new LinkedHashSet<>());
        lockout.playerDamageTypesTaken.get(player.getUUID()).add(source.typeHolder().unwrapKey().orElse(null));

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
                if (source.is(DamageTypes.ARROW)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " got hit by Arrow.");
                }
            }
            if (goal instanceof OpponentHitByWindChargeGoal) {
                if (source.is(DamageTypes.WIND_CHARGE)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " got hit by Wind Charge.");
                }
            }
            if (goal instanceof OpponentTakesFallDamageGoal) {
                if (source.is(DamageTypes.FALL)) {
                    lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " took fall damage.");
                }
            }
            if (goal instanceof OpponentTakes100DamageGoal) {
                if (lockout.damageTaken.get(team) >= 100) {
                    lockout.complete1v1Goal(goal, team, false, team.getDisplayName() + " took 100 damage.");
                }
            }

            if (goal instanceof DamagedByUniqueSourcesGoal damagedGoal) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> damageTypeKey = source.typeHolder().unwrapKey().orElse(null);

                if (damageTypeKey != null) {
                    lockout.damageTypesTaken.computeIfAbsent(team, t -> new java.util.LinkedHashSet<>());

                    boolean added = lockout.damageTypesTaken.get(team).add(damageTypeKey);
                    if (added) {
                        lockout.damageByUniqueSources.putIfAbsent(team, 0);
                        lockout.damageByUniqueSources.merge(team, 1, Integer::sum);
                        
                        // Track first contributor
                        lockout.firstDamageTypeContributor.putIfAbsent(team, new java.util.HashMap<>());
                        lockout.firstDamageTypeContributor.get(team).put(damageTypeKey, player.getUUID());
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

    @Inject(method = "awardStat(Lnet/minecraft/resources/Identifier;)V", at = @At("HEAD"))
    public void onIncrementStat(Identifier stat, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

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

    @Inject(method = "awardStat(Lnet/minecraft/resources/Identifier;I)V", at = @At("HEAD"))
    public void onIncreaseStat(Identifier stat, int amount, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (goal instanceof Sprint1KmGoal && stat.equals(Stats.SPRINT_ONE_CM)) {
                lockout.distanceSprinted.putIfAbsent(player.getUUID(), 0);
                lockout.distanceSprinted.merge(player.getUUID(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUUID())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                    team.sendTooltipUpdate((Sprint1KmGoal) goal);
                }

                if (lockout.distanceSprinted.get(player.getUUID()) >= (100 * 1000)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Crouch100mGoal && stat.equals(Stats.CROUCH_ONE_CM)) {
                lockout.distanceCrouched.putIfAbsent(player.getUUID(), 0);
                lockout.distanceCrouched.merge(player.getUUID(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUUID())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                    team.sendTooltipUpdate((Crouch100mGoal) goal);
                }

                if (lockout.distanceCrouched.get(player.getUUID()) >= (100 * 100)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Swim500mGoal && stat.equals(Stats.SWIM_ONE_CM)) {
                lockout.distanceSwam.putIfAbsent(player.getUUID(), 0);
                lockout.distanceSwam.merge(player.getUUID(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUUID())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                    team.sendTooltipUpdate((Swim500mGoal) goal);
                }

                if (lockout.distanceSwam.get(player.getUUID()) >= (100 * 500)) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof Boat2KmGoal && stat.equals(Stats.BOAT_ONE_CM)) {
                lockout.distanceByBoat.putIfAbsent(player.getUUID(), 0);
                lockout.distanceByBoat.merge(player.getUUID(), amount, Integer::sum);

                if (lockout.isLockoutPlayer(player.getUUID())) {
                    LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                    team.sendTooltipUpdate((Boat2KmGoal) goal);
                }

                if (lockout.distanceByBoat.get(player.getUUID()) >= (100 * 2000)) {
                    lockout.completeGoal(goal, player);
                }
            }
            
        }
    }

    @Inject(method = "giveExperienceLevels", at = @At("TAIL"))
    public void onExperienceLevelUp(int levels, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

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

    @Inject(method = "blockUsingItem", at = @At(value = "TAIL"))
    public void onTakeShieldHit(ServerLevel world, LivingEntity attacker, DamageSource damageSource, float damage, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        float f = attacker.getSecondsToDisableBlocking();

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            if (f <= 0.0F) continue;

            if (goal instanceof HaveShieldDisabledGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;)V", at = @At("HEAD"))
    private void onIncrementStat(Stat<?> stat, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        int currentValue = serverPlayer.getStats().getValue(stat);

        if (currentValue == 0) {
            if (stat.getType() == Stats.ITEM_USED) {
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
            if (stat.getType() == Stats.ITEM_BROKEN) {
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
