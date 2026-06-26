package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the entire inventory (including off-hand and armor slots) for Item types
 */
public abstract class ObtainAllItemsGoal extends ObtainItemsGoal {

    public ObtainAllItemsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public boolean satisfiedBy(Inventory playerInventory) {
        List<Item> items = new ArrayList<>(getItems());

        for (var equipmentSlot : EquipmentSlot.values())
        {
            var item = ((PlayerInventoryAccessor) playerInventory).getEquipment().get(equipmentSlot);
            if (item == null) continue;

            if (CheckRequiredAmount(item, playerInventory, items))
            {
                return true;
            }
        }

        for (ItemStack item : ((PlayerInventoryAccessor) playerInventory).getPlayerInventory()) {
            if (item == null) continue;

            if (CheckRequiredAmount(item, playerInventory, items))
            {
                return true;
            }
        }

        return false;
    }

    private boolean CheckRequiredAmount(ItemStack item, Inventory playerInventory, List<Item> items)
    {
        var allow = true;
        if (this instanceof RequiresAmount requiresAmount) {
            allow = playerInventory.countItem(item.getItem()) >= requiresAmount.getAmount();
        }
        if (allow && items.remove(item.getItem())) {
            return items.isEmpty();
        }

        return false;
    }
}
