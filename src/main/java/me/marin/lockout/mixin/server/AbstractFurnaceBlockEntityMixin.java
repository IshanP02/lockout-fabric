package me.marin.lockout.mixin.server;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.marin.lockout.lockout.interfaces.FurnaceSmeltTracker;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin implements FurnaceSmeltTracker {

    @Shadow
    private DefaultedList<ItemStack> inventory;

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
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/AbstractFurnaceBlockEntity;craftRecipe(Lnet/minecraft/registry/DynamicRegistryManager;Lnet/minecraft/recipe/RecipeEntry;Lnet/minecraft/recipe/input/SingleStackRecipeInput;Lnet/minecraft/util/collection/DefaultedList;I)Z"
        )
    )
    private static void lockout$captureInputBeforeCraft(
            net.minecraft.server.world.ServerWorld world,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.BlockState state,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {
        if (!(furnace instanceof FurnaceSmeltTracker tracker)) return;
        
        // Get the input stack (slot 0 in furnace inventory)
        ItemStack inputStack = furnace.getStack(0);
        if (inputStack.isEmpty()) return;
        
        tracker.lockout$setLastSmelted(inputStack.getItem());
    }
}
