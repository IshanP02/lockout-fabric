package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.opponent.OpponentCatchesOnFireGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "setSharedFlagOnFire", at = @At("HEAD"))
    public void setOnFire(boolean onFire, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!((Entity) (Object) this instanceof Player player) || !onFire) return;
        if (player.level().isClientSide()) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof OpponentCatchesOnFireGoal) {
                lockout.complete1v1Goal(goal, player, false, player.getName().getString() + " caught on fire.");
            }
        }
    }

}
