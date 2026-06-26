package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.workstation.LockMapUsingCartographyTableGoal;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.server.handlers.HorseArmorEquipHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {

    // Snapshot of horse armor slot before click
    private ItemStack lockout$prevHorseArmorStack = ItemStack.EMPTY;

    /* ---------------- Capture BEFORE click ---------------- */

    @Inject(method = "clicked", at = @At("HEAD"))
    private void lockout$onSlotClickHead(
            int slotIndex,
            int button,
            ContainerInput actionType,
            Player player,
            CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer)) return;

        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof HorseInventoryMenu horseHandler) {
            // Horse armor slot is ALWAYS index 1
            Slot horseArmorSlot = horseHandler.getSlot(1);
            if (horseArmorSlot != null) {
                lockout$prevHorseArmorStack = horseArmorSlot.getItem().copy();
            }
        }
    }

    /* ---------------- Process AFTER click ---------------- */

    @Inject(method = "clicked", at = @At("RETURN"))
    private void lockout$onSlotClickReturn(
            int slotIndex,
            int button,
            ContainerInput actionType,
            Player player,
            CallbackInfo ci
    ) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        AbstractContainerMenu handler = player.containerMenu;
        if (handler == null) return;

        // --- Cartography table ---
        if (handler instanceof CartographyTableMenu) {
            handleCartographyClick(player, slotIndex, actionType);
        }

        // --- Horse armor ---
        else if (handler instanceof HorseInventoryMenu horseHandler) {
            handleHorseArmorPlacement(serverPlayer, horseHandler, actionType);
        }
    }

    /* ---------------- Horse armor logic ---------------- */

   private void handleHorseArmorPlacement(
        ServerPlayer player,
        HorseInventoryMenu handler,
        ContainerInput actionType
) {
    // Ignore creative-only and nonsense actions
    if (actionType == ContainerInput.CLONE) {
        lockout$prevHorseArmorStack = ItemStack.EMPTY;
        return;
    }

    // Horse armor slot is index 1
    Slot horseArmorSlot = handler.getSlot(1);
    if (horseArmorSlot == null) {
        lockout$prevHorseArmorStack = ItemStack.EMPTY;
        return;
    }

    ItemStack currentStack = horseArmorSlot.getItem();

    boolean beforeWasArmor = isHorseArmor(lockout$prevHorseArmorStack);
    boolean afterIsArmor = isHorseArmor(currentStack);

    // New placement
    if (!beforeWasArmor && afterIsArmor) {
        HorseArmorEquipHandler.checkAndCompleteHorseArmorGoal(player, currentStack);
    }

    // Replacement (INCLUDING dyed leather → dyed leather)
    else if (beforeWasArmor && afterIsArmor
            && !ItemStack.isSameItemSameComponents(lockout$prevHorseArmorStack, currentStack)) {
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

    private void handleCartographyClick(Player player, int slotIndex, ContainerInput actionType) {
        if (slotIndex != 2) return;
        if (actionType != ContainerInput.PICKUP && actionType != ContainerInput.QUICK_MOVE) return;

        player.level().getServer().execute(() -> checkForLockedMap(player));
    }

    private static void checkForLockedMap(Player player) {
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ServerLevel world = (ServerLevel) player.level();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() != Items.FILLED_MAP) continue;

            MapId id = stack.get(DataComponents.MAP_ID);
            if (id == null) continue;

            MapItemSavedData state = world.getMapData(id);
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
