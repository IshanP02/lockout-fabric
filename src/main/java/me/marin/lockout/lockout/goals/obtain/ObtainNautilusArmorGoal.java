package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class ObtainNautilusArmorGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR, Items.DIAMOND_NAUTILUS_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR);

    public ObtainNautilusArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain Nautilus Armor";
    }

    @Override
    public int getAmount() {
        return 1;
    }
}
