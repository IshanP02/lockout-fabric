package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillDecoratedPotGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
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

@Mixin(DecoratedPotBlock.class)
public class DecoratedPotBlockMixin {

    @Inject(
        method = "useItemOn",
        at = @At("RETURN")
    )
    private void lockout$checkPotFilled(
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
        if (cir.getReturnValue() != InteractionResult.SUCCESS) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        if (!(world.getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot)) return;

        ItemStack stored = pot.getItem(0);

        if (stored.isEmpty()) return;
        if (stored.getCount() < stored.getMaxStackSize()) return;

        // Pot now contains a full stack
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof FillDecoratedPotGoal)) continue;
            if (goal.isCompleted()) continue;

            lockout.completeGoal(goal, player);
        }
    }
}