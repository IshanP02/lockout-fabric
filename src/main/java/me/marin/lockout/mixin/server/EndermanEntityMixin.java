package me.marin.lockout.mixin.server;
import me.marin.lockout.lockout.goals.misc.AngerEndermanGoal;
import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import net.minecraft.entity.mob.EndermanEntity;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

       
@Mixin(EndermanEntity.class)
public class EndermanEntityMixin {

    @Inject(method = "setAngryAt", at = @At("HEAD"))
    public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt, CallbackInfo ci) {
        EndermanEntity enderman = (EndermanEntity) (Object) this;
        if (enderman.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            return;
        }

        ServerPlayerEntity player;
        try {
            LivingEntity target = angryAt.getEntityByClass(enderman.getEntityWorld(), LivingEntity.class);
            if (target instanceof ServerPlayerEntity serverPlayer) {
                 player = serverPlayer;
            } else {
                 return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof AngerEndermanGoal)) continue;
            if (goal.isCompleted()) continue;

            lockout.completeGoal(goal, player);
            return;
        }
    }
}
