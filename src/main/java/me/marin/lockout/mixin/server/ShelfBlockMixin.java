package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillShelfWithShelvesGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShelfBlock.class)
public class ShelfBlockMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onInsertShelf(
        ItemStack stack,
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (world.isClientSide()) return;
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ShelfBlockEntity shelf)) return;
        
        if (!cir.getReturnValue().consumesAction()) return;

        // Check if all 3 slots are filled with shelves (any wood variant)
        int shelfCount = 0;
        for (int i = 0; i < shelf.getContainerSize(); i++) {
            ItemStack slotStack = shelf.getItem(i);
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
        return stack.is(Items.OAK_SHELF) ||
               stack.is(Items.SPRUCE_SHELF) ||
               stack.is(Items.BIRCH_SHELF) ||
               stack.is(Items.JUNGLE_SHELF) ||
               stack.is(Items.ACACIA_SHELF) ||
               stack.is(Items.DARK_OAK_SHELF) ||
               stack.is(Items.MANGROVE_SHELF) ||
               stack.is(Items.CHERRY_SHELF) ||
               stack.is(Items.CRIMSON_SHELF) ||
               stack.is(Items.WARPED_SHELF) ||
               stack.is(Items.PALE_OAK_SHELF) ||
               stack.is(Items.BAMBOO_SHELF);
    }
}
