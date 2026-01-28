package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class ObtainCopperToolsGoal extends ObtainAllItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.COPPER_AXE, Items.COPPER_HOE, Items.COPPER_PICKAXE, Items.COPPER_SWORD, Items.COPPER_SHOVEL, Items.COPPER_SPEAR);

    public ObtainCopperToolsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain all Copper Tools";
    }

}

