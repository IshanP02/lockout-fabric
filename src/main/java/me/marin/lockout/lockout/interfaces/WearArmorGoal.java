package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class WearArmorGoal extends ObtainAllItemsGoal {

    public WearArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public boolean satisfiedBy(Inventory playerInventory) {
        List<Item> items = new ArrayList<>(getItems());

        // TODO: Do better
        var armor = new ArrayList<ItemStack>();
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.HEAD));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.CHEST));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.LEGS));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.FEET));

        for (ItemStack item : armor) {
            if (item == null) continue;
            if (items.remove(item.getItem())) {
                if (items.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

}
