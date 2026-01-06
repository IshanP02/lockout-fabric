package me.marin.lockout.lockout.goals.workstation;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class LockMapUsingCartographyTableGoal extends Goal {

    public LockMapUsingCartographyTableGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Lock a Map Using Cartography Table";
    }

    private static final ItemStack ITEM_STACK = Items.CARTOGRAPHY_TABLE.getDefaultStack();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
