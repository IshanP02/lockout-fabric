package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.status_effect.GetXStatusEffectsGoal;
import me.marin.lockout.lockout.interfaces.StatusEffectGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffect.class)
public class StatusEffectMixin {

    @Inject(method = "onEffectAdded", at = @At("HEAD"))
    public void onApplied(LivingEntity entity, int amplifier, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            MobEffect statusEffect = (MobEffect) (Object) this;
            if (goal instanceof StatusEffectGoal statusEffectGoal) {
                if (statusEffectGoal.getStatusEffect().equals(statusEffect)) {
                    lockout.completeGoal(statusEffectGoal, player);
                }
            }
            if (goal instanceof GetXStatusEffectsGoal getXStatusEffectsGoal) {
                if (player.getActiveEffects().size() >= getXStatusEffectsGoal.getAmount()) {
                    lockout.completeGoal(goal, player);
                }
            }
            
        }
    }

}
