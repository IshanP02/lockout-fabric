package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.interfaces.ObtainItemsGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FillInventoryWithUniqueItemsGoal extends ObtainItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.CHEST);
    public FillInventoryWithUniqueItemsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Fill Inventory with unique items";
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public boolean satisfiedBy(Inventory playerInventory) {
        Set<Item> itemTypes = new HashSet<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = playerInventory.getItem(i);
            if (item == null || item.isEmpty()) return false;

            if (!itemTypes.add(item.getItem())) return false;
        }
        return (itemTypes.size() == 36);
    }

}
