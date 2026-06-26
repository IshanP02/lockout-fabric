package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.ObtainPotionItemGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancements.triggers.BrewedPotionTrigger;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewedPotionTrigger.class)
public class BrewedPotionCriterionMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    public void onTrigger(ServerPlayer player, Holder<Potion> potion, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ObtainPotionItemGoal obtainPotionItemGoal) {
                if (obtainPotionItemGoal.getPotions().contains(potion)) {
                    lockout.completeGoal(obtainPotionItemGoal, player);
                }
            }
        }
    }

}
