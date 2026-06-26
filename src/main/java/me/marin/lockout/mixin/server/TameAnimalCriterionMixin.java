package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.TameAnimalGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TameAnimalTrigger.class)
public class TameAnimalCriterionMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    public void onTameAnimal(ServerPlayer player, Animal entity, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof TameAnimalGoal tameAnimalGoal)) continue;
            if (goal.isCompleted()) continue;

            if (tameAnimalGoal.matchesAnimal(entity.getType())) {
                lockout.completeGoal(tameAnimalGoal, player);
                return;
            }
        }
    }

}
