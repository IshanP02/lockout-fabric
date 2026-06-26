package me.marin.lockout.mixin.server;
import me.marin.lockout.lockout.goals.misc.AngerZombifiedPiglinGoal;
import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.AngerZombifiedPiglinGoal;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

       
@Mixin(ZombifiedPiglin.class)
public class ZombifiedPiglinEntityMixin {

    @Inject(method = "setPersistentAngerTarget", at = @At("HEAD"))
    public void setAngryAt(@Nullable EntityReference<LivingEntity> angryAt, CallbackInfo ci) {
        ZombifiedPiglin pigman = (ZombifiedPiglin) (Object) this;
        if (pigman.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            return;
        }

        ServerPlayer player;
        try {
            LivingEntity target = angryAt.getEntity(pigman.level(), LivingEntity.class);
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
            if (!(goal instanceof AngerZombifiedPiglinGoal)) continue;
            if (goal.isCompleted()) continue;

            lockout.completeGoal(goal, player);
            return;
        }
    }
}
