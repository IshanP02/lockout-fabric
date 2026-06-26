package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.Deal400DamageGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurtServer", at = @At("RETURN"))
    public void onDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(source.getEntity() instanceof Player player) || !cir.getReturnValue()) return;
        if (player.level().isClientSide()) return;

        if (!lockout.isLockoutPlayer(player.getUUID())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
        lockout.damageDealt.putIfAbsent(team, 0d);
        lockout.damageDealt.merge(team, (double)amount, Double::sum);
        
        // Track per-player for statistics
        lockout.playerDamageDealt.putIfAbsent(player.getUUID(), 0d);
        lockout.playerDamageDealt.merge(player.getUUID(), (double)amount, Double::sum);

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof Deal400DamageGoal deal400DamageGoal) {
                team.sendTooltipUpdate(deal400DamageGoal);
                if (lockout.damageDealt.get(team) >= 400) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

}
