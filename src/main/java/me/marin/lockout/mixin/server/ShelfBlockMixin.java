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
        System.out.println("DEBUG: ShelfBlockMixin triggered");
        
        if (world.isClient()) {
            System.out.println("DEBUG: Client side, returning");
            return;
        }
        
        System.out.println("DEBUG: Server side");
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) {
            System.out.println("DEBUG: Lockout not running");
            return;
        }
        
        System.out.println("DEBUG: Lockout is running");

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ShelfBlockEntity shelf)) {
            System.out.println("DEBUG: Not a ShelfBlockEntity, type: " + (blockEntity != null ? blockEntity.getClass().getName() : "null"));
            return;
        }
        
        System.out.println("DEBUG: Is ShelfBlockEntity");
        
        if (!cir.getReturnValue().isAccepted()) {
            System.out.println("DEBUG: Action result not accepted: " + cir.getReturnValue());
            return;
        }
        
        System.out.println("DEBUG: Action result is accepted");

        // Check if all 3 slots are filled with shelves (any wood variant)
        int shelfCount = 0;
        for (int i = 0; i < shelf.size(); i++) {
            ItemStack slotStack = shelf.getStack(i);
            System.out.println("DEBUG: Slot " + i + " - isEmpty: " + slotStack.isEmpty() + ", isShelf: " + (slotStack.isEmpty() ? "N/A" : isShelf(slotStack)) + ", item: " + (slotStack.isEmpty() ? "empty" : slotStack.getItem()));
            if (!slotStack.isEmpty() && isShelf(slotStack)) {
                shelfCount++;
            }
        }
        
        System.out.println("DEBUG: Shelf count: " + shelfCount + "/" + shelf.size());

        if (shelfCount < 3) {
            System.out.println("DEBUG: Not enough shelves, need 3");
            return;
        }
        
        System.out.println("DEBUG: All 3 slots filled with shelves!");

        System.out.println("DEBUG: All 3 slots filled with shelves!");

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillShelfWithShelvesGoal) {
                System.out.println("DEBUG: Completing FillShelfWithShelvesGoal for player " + player.getName().getString());
                lockout.completeGoal(goal, player);
                return;
            }
        }
        
        System.out.println("DEBUG: No FillShelfWithShelvesGoal found in goals");
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
