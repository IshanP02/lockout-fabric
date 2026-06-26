package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.ConstructCopperGolemGoal;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static me.marin.lockout.server.LockoutServer.lockout;

public class CopperGolemConstructionHandler implements UseBlockCallback {

    // Tracks recent pumpkin placements: BlockPos -> (Player, tickTime)
    private static final Map<BlockPos, PumpkinPlacement> RECENT_PUMPKIN_PLACEMENTS = new HashMap<>();
    
    // Maximum ticks to keep a placement record (5 ticks = ~0.25 seconds)
    private static final int MAX_PLACEMENT_AGE_TICKS = 5;
    
    private record PumpkinPlacement(ServerPlayer player, long tickTime) {}
    public static void recordPumpkinAction(BlockPos pos, ServerPlayer player, Level world) {
        if (!Lockout.isLockoutRunning(lockout)) return;
        RECENT_PUMPKIN_PLACEMENTS.put(pos, new PumpkinPlacement(player, world.getGameTime()));
    }
    
    @Override
    public InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!Lockout.isLockoutRunning(lockout)) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        
        BlockPos clickedPos = blockHitResult.getBlockPos();
        BlockPos placementPos = clickedPos.above();
        
        // Check if player is placing a carved pumpkin or jack o' lantern
        if (player.getItemInHand(hand).is(Blocks.CARVED_PUMPKIN.asItem()) ||
            player.getItemInHand(hand).is(Blocks.JACK_O_LANTERN.asItem())) {
            
            // Record this pumpkin placement with current world time
            recordPumpkinAction(placementPos, serverPlayer, world);
        }
        
        return InteractionResult.PASS;
    }
    
    public static ServerPlayer findConstructor(BlockPos golemPos, Level world) {
        if (!Lockout.isLockoutRunning(lockout)) return null;
        
        long currentTime = world.getGameTime();
        ServerPlayer constructor = null;
        
        // Check for recent pumpkin placements within 2.5 blocks of the golem spawn
        Iterator<Map.Entry<BlockPos, PumpkinPlacement>> iterator = RECENT_PUMPKIN_PLACEMENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, PumpkinPlacement> entry = iterator.next();
            BlockPos pumpkinPos = entry.getKey();
            PumpkinPlacement placement = entry.getValue();
            
            // Remove old entries
            if (currentTime - placement.tickTime() > MAX_PLACEMENT_AGE_TICKS) {
                iterator.remove();
                continue;
            }
            
            // Check if this pumpkin is close to the golem spawn position
            if (pumpkinPos.closerThan(golemPos, 2.5)) {
                constructor = placement.player();
                iterator.remove(); // Remove used entry
                break;
            }
        }
        
        return constructor;
    }
    
    /**
     * Call this when a copper golem is successfully spawned
     */
    public static void onCopperGolemSpawn(ServerPlayer player) {
        if (!Lockout.isLockoutRunning(lockout)) return;
        
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;
            
            if (goal instanceof ConstructCopperGolemGoal) {
                lockout.completeGoal(goal, player);
            }
        }
    }
}
