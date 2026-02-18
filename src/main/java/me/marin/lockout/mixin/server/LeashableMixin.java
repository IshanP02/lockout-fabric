package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.interfaces.LeashUniqueEntitiesAtOnceGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Leashable.class)
public interface LeashableMixin {

    @Inject(
        method = "attachLeash",
        at = @At("HEAD")
    )
    default void lockout$onAttachLeash(
            Entity leashHolder,
            boolean sendPacket,
            CallbackInfo ci
    ) {

        Entity entity = (Entity) (Object) this;

        // Server-only
        if (entity.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        // If leashing to a non-player (fence, etc.), remove from all players' tracking
        if (!(leashHolder instanceof PlayerEntity)) {
            // Remove this entity type from all players who were tracking it
            LeashUniqueEntitiesAtOnceGoal.removeEntityTypeFromAllPlayers(entity.getType());
            return;
        }

        // Only player-held leashes from here on
        PlayerEntity player = (PlayerEntity) leashHolder;

        // Track unique mob types for this player
        LeashUniqueEntitiesAtOnceGoal.addLeashedType(player.getUuid(), entity.getType());
        int uniqueCount = LeashUniqueEntitiesAtOnceGoal.getUniqueLeashedCount(player.getUuid());

        // Check all goals
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null || goal.isCompleted()) continue;

            // Check specific mob leash goals
            if (goal instanceof LeashMobGoal leashGoal) {
                if (leashGoal.matchesMob(entity)) {
                    lockout.completeGoal(goal, player);
                    break;
                }
            }
            
            // Check unique types goal
            if (goal instanceof LeashUniqueEntitiesAtOnceGoal uniqueGoal) {
                if (uniqueCount >= uniqueGoal.getRequiredUniqueTypes()) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

    @Inject(
        method = "detachLeash",
        at = @At("HEAD")
    )
    default void lockout$onDetachLeash(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // Server-only
        if (entity.getEntityWorld().isClient()) return;

        if (!(entity instanceof Leashable leashable)) return;
        Entity leashHolder = leashable.getLeashHolder();
        if (!(leashHolder instanceof PlayerEntity player)) return;

        // Remove this mob type from player's tracked leashes
        LeashUniqueEntitiesAtOnceGoal.removeLeashedType(player.getUuid(), entity.getType());
    }
}
