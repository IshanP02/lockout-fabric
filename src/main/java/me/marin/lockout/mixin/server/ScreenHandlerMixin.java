package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.workstation.LockMapUsingCartographyTableGoal;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.server.handlers.HorseArmorEquipHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    // Snapshot of horse armor slot before click
    private ItemStack lockout$prevHorseArmorStack = ItemStack.EMPTY;

    /* ---------------- Capture BEFORE click ---------------- */

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void lockout$onSlotClickHead(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayerEntity)) return;

        ScreenHandler handler = player.currentScreenHandler;
        if (handler instanceof HorseScreenHandler horseHandler) {
            // Horse armor slot is ALWAYS index 1
            Slot horseArmorSlot = horseHandler.getSlot(1);
            if (horseArmorSlot != null) {
                lockout$prevHorseArmorStack = horseArmorSlot.getStack().copy();
            }
        }
    }

    /* ---------------- Process AFTER click ---------------- */

    @Inject(method = "onSlotClick", at = @At("RETURN"))
    private void lockout$onSlotClickReturn(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        if (player.getEntityWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return;

        // --- Cartography table ---
        if (handler instanceof CartographyTableScreenHandler) {
            handleCartographyClick(player, slotIndex, actionType);
        }

        // --- Horse armor ---
        else if (handler instanceof HorseScreenHandler horseHandler) {
            handleHorseArmorPlacement(serverPlayer, horseHandler, actionType);
        }
    }

    /* ---------------- Horse armor logic ---------------- */

   private void handleHorseArmorPlacement(
        ServerPlayerEntity player,
        HorseScreenHandler handler,
        SlotActionType actionType
) {
    // Ignore creative-only and nonsense actions
    if (actionType == SlotActionType.CLONE) {
        lockout$prevHorseArmorStack = ItemStack.EMPTY;
        return;
    }

    // Horse armor slot is index 1
    Slot horseArmorSlot = handler.getSlot(1);
    if (horseArmorSlot == null) {
        lockout$prevHorseArmorStack = ItemStack.EMPTY;
        return;
    }

    ItemStack currentStack = horseArmorSlot.getStack();

    boolean beforeWasArmor = isHorseArmor(lockout$prevHorseArmorStack);
    boolean afterIsArmor = isHorseArmor(currentStack);

    // New placement
    if (!beforeWasArmor && afterIsArmor) {
        HorseArmorEquipHandler.checkAndCompleteHorseArmorGoal(player, currentStack);
    }

    // Replacement (INCLUDING dyed leather → dyed leather)
    else if (beforeWasArmor && afterIsArmor
            && !ItemStack.areEqual(lockout$prevHorseArmorStack, currentStack)) {
        HorseArmorEquipHandler.checkAndCompleteHorseArmorGoal(player, currentStack);
    }

    lockout$prevHorseArmorStack = ItemStack.EMPTY;
}

    private boolean isHorseArmor(ItemStack stack) {
        return stack != null && (
                stack.getItem() == Items.LEATHER_HORSE_ARMOR ||
                stack.getItem() == Items.IRON_HORSE_ARMOR ||
                stack.getItem() == Items.GOLDEN_HORSE_ARMOR ||
                stack.getItem() == Items.DIAMOND_HORSE_ARMOR
        );
    }

    /* ---------------- Cartography table ---------------- */

    private void handleCartographyClick(PlayerEntity player, int slotIndex, SlotActionType actionType) {
        if (slotIndex != 2) return;
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        player.getEntityWorld().getServer().execute(() -> checkForLockedMap(player));
    }

    private static void checkForLockedMap(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ServerWorld world = (ServerWorld) player.getEntityWorld();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() != Items.FILLED_MAP) continue;

            MapIdComponent id = stack.get(DataComponentTypes.MAP_ID);
            if (id == null) continue;

            MapState state = world.getMapState(id);
            if (state != null && state.locked) {
                for (Goal goal : lockout.getBoard().getGoals()) {
                    if (goal instanceof LockMapUsingCartographyTableGoal && !goal.isCompleted()) {
                        lockout.completeGoal(goal, player);
                        return;
                    }
                }
            }
        }
    }
}
