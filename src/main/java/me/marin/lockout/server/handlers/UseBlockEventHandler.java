package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.LightCandleGoal;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import static me.marin.lockout.server.LockoutServer.lockout;

public class UseBlockEventHandler implements UseBlockCallback {

    @Override
    public InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!Lockout.isLockoutRunning(lockout)) return InteractionResult.PASS;

        BlockPos blockPos = blockHitResult.getBlockPos();
        var candleState = world.getBlockState(blockPos);
        if (!(candleState.getBlock() instanceof CandleBlock) || candleState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) return InteractionResult.PASS;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof LightCandleGoal) {
                lockout.completeGoal(goal, player);
            }
        }
        return InteractionResult.PASS;
    }
}
