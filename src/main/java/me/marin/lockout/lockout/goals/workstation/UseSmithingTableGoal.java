package me.marin.lockout.lockout.goals.workstation;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class UseSmithingTableGoal extends Goal {

    public UseSmithingTableGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Smithing Table";
    }

    private static final ItemStack ITEM_STACK = Items.SMITHING_TABLE.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
