package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillShelfWithShelvesGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShelfBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {

    @Inject(method = "onUseWithItem", at = @At("RETURN"))
    private void onInsertShelf(
        ItemStack stack,
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        Hand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient()) return;
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ShelfBlockEntity shelf)) return;
        
        if (!cir.getReturnValue().isAccepted()) return;

        // Check if all 3 slots are filled with shelves (any wood variant)
        int shelfCount = 0;
        for (int i = 0; i < shelf.size(); i++) {
            ItemStack slotStack = shelf.getStack(i);
            if (!slotStack.isEmpty() && isShelf(slotStack)) {
                shelfCount++;
            }
        }

        if (shelfCount < 3) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillShelfWithShelvesGoal) {
                lockout.completeGoal(goal, player);
                return;
            }
        }
    }

    private boolean isShelf(ItemStack stack) {
        return stack.isOf(Items.OAK_SHELF) ||
               stack.isOf(Items.SPRUCE_SHELF) ||
               stack.isOf(Items.BIRCH_SHELF) ||
               stack.isOf(Items.JUNGLE_SHELF) ||
               stack.isOf(Items.ACACIA_SHELF) ||
               stack.isOf(Items.DARK_OAK_SHELF) ||
               stack.isOf(Items.MANGROVE_SHELF) ||
               stack.isOf(Items.CHERRY_SHELF) ||
               stack.isOf(Items.CRIMSON_SHELF) ||
               stack.isOf(Items.WARPED_SHELF) ||
               stack.isOf(Items.PALE_OAK_SHELF) ||
               stack.isOf(Items.BAMBOO_SHELF);
    }
}
