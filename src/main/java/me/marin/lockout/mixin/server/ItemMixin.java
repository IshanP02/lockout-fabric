package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("HEAD"))
    public void onUseCompass(Level world, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ItemStack stack = player.getItemInHand(hand);

        if (stack == null || stack.isEmpty()) return;
        if (stack.getItem() != Items.COMPASS) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.copyTag().contains("PlayerTracker")) {
            LockoutServer.compassHandler.cycle(player);
        }
    }

}
