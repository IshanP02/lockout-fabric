package me.marin.lockout.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.ShootFireworkFromCrossbowGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;performShooting(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;FFLnet/minecraft/world/entity/LivingEntity;)V"))
    public void onShoot(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Local ChargedProjectiles ChargedProjectiles) {
        if (world.isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        boolean hasRocket = ChargedProjectiles.contains(Items.FIREWORK_ROCKET);
        if (!hasRocket) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ShootFireworkFromCrossbowGoal) {
                lockout.completeGoal(goal, user);
                return;
            }
        }
    }

}
