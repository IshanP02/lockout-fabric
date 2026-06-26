package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillChiseledBookshelfGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChiseledBookShelfBlock.class)
public class ChiseledBookshelfBlockMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    public void onUseWithItem(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (world.isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ChiseledBookShelfBlockEntity blockEntity = (ChiseledBookShelfBlockEntity) world.getBlockEntity(pos);
        long filledSlots = blockEntity.getItems().stream().filter(s -> !s.isEmpty()).count();
        if (cir.getReturnValue() != InteractionResult.SUCCESS || filledSlots < 6) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof FillChiseledBookshelfGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
