package me.marin.lockout.mixin.server;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.marin.lockout.lockout.interfaces.FurnaceSmeltTracker;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin implements FurnaceSmeltTracker {

    @Unique
    private Item lockout$lastSmelted;

    @Override
    public void lockout$setLastSmelted(Item item) {
        this.lockout$lastSmelted = item;
    }

    @Override
    public Item lockout$getLastSmelted() {
        return this.lockout$lastSmelted;
    }

    @Inject(
        method = "serverTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private static void lockout$captureInputBeforeCraft(
            net.minecraft.server.level.ServerLevel world,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {
        if (!(furnace instanceof FurnaceSmeltTracker tracker)) return;

        ItemStack inputStack = furnace.getItem(0);
        if (inputStack.isEmpty()) return;

        tracker.lockout$setLastSmelted(inputStack.getItem());
    }
}
