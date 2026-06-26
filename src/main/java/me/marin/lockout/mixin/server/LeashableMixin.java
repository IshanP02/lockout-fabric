package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.interfaces.LeashUniqueEntitiesAtOnceGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class LeashableMixin {

    @Inject(method = "setLeashData", at = @At("HEAD"))
    public void lockout$onSetLeashData(Leashable.LeashData newData, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide()) return;

        Leashable.LeashData oldData = ((Mob) (Object) this).getLeashData();

        // Leash being removed — untrack from whichever player held it
        if (newData == null && oldData != null && oldData.leashHolder instanceof Player player) {
            LeashUniqueEntitiesAtOnceGoal.removeLeashedType(player.getUUID(), entity.getType());
            return;
        }

        // Leash being attached
        if (newData != null && newData.leashHolder != null) {
            Lockout lockout = LockoutServer.lockout;
            if (!Lockout.isLockoutRunning(lockout)) return;

            // Leashed to a non-player (fence post, etc.) — remove from all player tracking
            if (!(newData.leashHolder instanceof Player)) {
                LeashUniqueEntitiesAtOnceGoal.removeEntityTypeFromAllPlayers(entity.getType());
                return;
            }

            Player player = (Player) newData.leashHolder;
            LeashUniqueEntitiesAtOnceGoal.addLeashedType(player.getUUID(), entity.getType());
            int uniqueCount = LeashUniqueEntitiesAtOnceGoal.getUniqueLeashedCount(player.getUUID());

            for (Goal goal : lockout.getBoard().getGoals()) {
                if (goal == null || goal.isCompleted()) continue;

                if (goal instanceof LeashMobGoal leashGoal) {
                    if (leashGoal.matchesMob(entity)) {
                        lockout.completeGoal(goal, player);
                        break;
                    }
                }

                if (goal instanceof LeashUniqueEntitiesAtOnceGoal uniqueGoal) {
                    if (uniqueCount >= uniqueGoal.getRequiredUniqueTypes()) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }
        }
    }
}
