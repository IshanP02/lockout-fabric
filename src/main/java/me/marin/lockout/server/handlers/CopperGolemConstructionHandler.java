package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.ConstructCopperGolemGoal;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static me.marin.lockout.server.LockoutServer.lockout;

public class CopperGolemConstructionHandler implements UseBlockCallback {

    // Tracks recent pumpkin placements: BlockPos -> (PlayerEntity, tickTime)
    private static final Map<BlockPos, PumpkinPlacement> RECENT_PUMPKIN_PLACEMENTS = new HashMap<>();
    
    // Maximum ticks to keep a placement record (5 ticks = ~0.25 seconds)
    private static final int MAX_PLACEMENT_AGE_TICKS = 5;
    
    private record PumpkinPlacement(ServerPlayerEntity player, long tickTime) {}
    public static void recordPumpkinAction(BlockPos pos, ServerPlayerEntity player, World world) {
        if (!Lockout.isLockoutRunning(lockout)) return;
        RECENT_PUMPKIN_PLACEMENTS.put(pos, new PumpkinPlacement(player, world.getTime()));
    }
    
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        if (!Lockout.isLockoutRunning(lockout)) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        
        BlockPos clickedPos = blockHitResult.getBlockPos();
        BlockPos placementPos = clickedPos.up();
        
        // Check if player is placing a carved pumpkin or jack o' lantern
        if (player.getStackInHand(hand).isOf(Blocks.CARVED_PUMPKIN.asItem()) ||
            player.getStackInHand(hand).isOf(Blocks.JACK_O_LANTERN.asItem())) {
            
            // Record this pumpkin placement with current world time
            recordPumpkinAction(placementPos, serverPlayer, world);
        }
        
        return ActionResult.PASS;
    }
    
    public static ServerPlayerEntity findConstructor(BlockPos golemPos, World world) {
        if (!Lockout.isLockoutRunning(lockout)) return null;
        
        long currentTime = world.getTime();
        ServerPlayerEntity constructor = null;
        
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
            if (pumpkinPos.isWithinDistance(golemPos, 2.5)) {
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
    public static void onCopperGolemSpawn(ServerPlayerEntity player) {
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
