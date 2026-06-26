package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.UseGlowInkGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.GlowInkSacItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlowInkSacItem.class)
public class GlowInkSacItemMixin {

    @Inject(method="tryApplyToSign", at = @At("RETURN"))
    public void useOnSign(Level world, SignBlockEntity signBlockEntity, boolean front, ItemStack stack, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!cir.getReturnValue()) return;

        Item signItem = world.getBlockState(signBlockEntity.getBlockPos()).getBlock().asItem();
        if (signItem != Items.CRIMSON_SIGN && signItem != Items.CRIMSON_HANGING_SIGN) {
            return;
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof UseGlowInkGoal) {
                lockout.completeGoal(goal, player);
            }
        }

    }

}
