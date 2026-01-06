package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.workstation.LockMapUsingCartographyTableGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient) return;

        // Only cartography tables
        if (!(player.currentScreenHandler instanceof CartographyTableScreenHandler)) {
            return;
        }

        // Output slot
        if (slotIndex != 2) return;

        // Normal click or shift-click
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) {
            return;
        }

        // Schedule check for next tick (map is locked AFTER click handling)
        player.getServer().execute(() -> checkForLockedMap(player));
    }

    private static void checkForLockedMap(PlayerEntity player) {
        if (player.getWorld().isClient) return;
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ServerWorld world = (ServerWorld) player.getWorld();

        // Scan player inventory for a locked map
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() != Items.FILLED_MAP) continue;

            MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
            if (mapIdComponent == null) continue;

            MapState mapState = world.getMapState(mapIdComponent);
            if (mapState != null && mapState.locked) {
                // Found a locked map - complete the goal
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal == null) continue;
                    if (goal.isCompleted()) continue;

                    if (goal instanceof LockMapUsingCartographyTableGoal) {
                        lockout.completeGoal(goal, player);
                        return;
                    }
                }
            }
        }
    }

}
