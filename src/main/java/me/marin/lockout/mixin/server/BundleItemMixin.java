package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillBundleWithBundlesGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(method = "overrideStackedOnOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BundleItem;broadcastChangesOnContainerMenu(Lnet/minecraft/world/entity/player/Player;)V"))
    public void onOverrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickAction, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        lockout$onClick(lockout, player, stack);
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BundleItem;broadcastChangesOnContainerMenu(Lnet/minecraft/world/entity/player/Player;)V"))
    public void onOverrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickAction, Player player, SlotAccess cursorSlotAccess, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        lockout$onClick(lockout, player, stack);
    }

    @Unique
    private static void lockout$onClick(Lockout lockout, Player player, ItemStack stack) {
        BundleContents bcc = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bcc == null) return;

        List<ItemStack> items = bcc.itemCopyStream().toList();
        int bundles = 0;
        for (ItemStack item : items) {
            if (!(item.getItem() instanceof BundleItem)) return;
            bundles++;
        }
        if (bundles < 16) {
            return;
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillBundleWithBundlesGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
