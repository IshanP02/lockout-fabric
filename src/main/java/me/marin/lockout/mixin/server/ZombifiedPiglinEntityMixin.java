package me.marin.lockout.mixin.server;
import me.marin.lockout.lockout.goals.misc.AngerZombifiedPiglinGoal;
import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.AngerZombifiedPiglinGoal;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

       
@Mixin(ZombifiedPiglinEntity.class)
public class ZombifiedPiglinEntityMixin {

    @Inject(method = "setAngryAt", at = @At("HEAD"))
    public void setAngryAt(@Nullable LazyEntityReference<LivingEntity> angryAt, CallbackInfo ci) {
        ZombifiedPiglinEntity pigman = (ZombifiedPiglinEntity) (Object) this;
        if (pigman.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            return;
        }

        ServerPlayerEntity player;
        try {
            LivingEntity target = angryAt.getEntityByClass(pigman.getEntityWorld(), LivingEntity.class);
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
            if (!(goal instanceof AngerZombifiedPiglinGoal)) continue;
            if (goal.isCompleted()) continue;

            lockout.completeGoal(goal, player);
            return;
        }
    }
}
