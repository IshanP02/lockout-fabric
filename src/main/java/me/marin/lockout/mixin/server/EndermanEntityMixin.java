package me.marin.lockout.mixin.server;
import me.marin.lockout.lockout.goals.misc.AngerEndermanGoal;
import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

       
@Mixin(EnderMan.class)
public class EndermanEntityMixin {

    @Inject(method = "setPersistentAngerTarget", at = @At("HEAD"))
    public void setAngryAt(@Nullable EntityReference<LivingEntity> angryAt, CallbackInfo ci) {
        EnderMan enderman = (EnderMan) (Object) this;
        if (enderman.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            return;
        }

        ServerPlayer player;
        try {
            LivingEntity target = angryAt.getEntity(enderman.level(), LivingEntity.class);
            if (target instanceof ServerPlayer serverPlayer) {
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
