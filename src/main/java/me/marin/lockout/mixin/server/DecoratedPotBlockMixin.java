package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillDecoratedPotGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.BlockState;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotBlock.class)
public class DecoratedPotBlockMixin {

    @Inject(
        method = "onUseWithItem",
        at = @At("RETURN")
    )
    private void lockout$checkPotFilled(
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
        if (cir.getReturnValue() != ActionResult.SUCCESS) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        if (!(world.getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot)) return;

        ItemStack stored = pot.getStack();

        if (stored.isEmpty()) return;
        if (stored.getCount() < stored.getMaxCount()) return;

        // Pot now contains a full stack
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof FillDecoratedPotGoal)) continue;
            if (goal.isCompleted()) continue;

            lockout.completeGoal(goal, player);
        }
    }
}