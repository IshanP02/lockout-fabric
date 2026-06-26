package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.UseBrushOnSuspiciousBlock;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushableBlockEntity.class)
public class BrushableBlockEntityMixin {

    @Inject(method = "brushingCompleted", at = @At("HEAD"))
    public void finishBrushing(ServerLevel world, LivingEntity brusher, ItemStack brush, CallbackInfo ci) {
        if (brusher instanceof Player player)
        {
            if (player.level().isClientSide()) return;
            Lockout lockout = LockoutServer.lockout;
            if (!Lockout.isLockoutRunning(lockout)) return;

            for (Goal goal : lockout.getBoard().getGoals()) {
                if (goal == null) continue;
                if (goal.isCompleted()) continue;

                if (goal instanceof UseBrushOnSuspiciousBlock) {
                    lockout.completeGoal(goal, player);
                }
            }
        }
    }

}
