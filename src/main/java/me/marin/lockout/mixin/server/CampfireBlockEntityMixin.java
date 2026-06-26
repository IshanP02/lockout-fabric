package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillCampfireWithFoodGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {

    @Inject(method = "placeFood", at = @At("RETURN"))
    public void addItem(ServerLevel world, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(entity instanceof Player player) || !cir.getReturnValueZ()) return;

        CampfireBlockEntity campfire = (CampfireBlockEntity) (Object) this;

        boolean filled = true;
        for (ItemStack itemStack : campfire.getItems()) {
            if (itemStack.isEmpty()) {
                filled = false;
                break;
            }
        }
        if (!filled) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillCampfireWithFoodGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
