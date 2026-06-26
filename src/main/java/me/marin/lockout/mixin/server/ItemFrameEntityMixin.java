package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.ItemFrameInItemFrameGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    public void onAddItem(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) return;
        ItemFrame itemFrame = (ItemFrame) (Object) this;
        if (itemFrame instanceof GlowItemFrame) return;

        ItemStack value = player.getItemInHand(hand);
        if (value.isEmpty() || !value.getItem().equals(Items.ITEM_FRAME)) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof ItemFrameInItemFrameGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
