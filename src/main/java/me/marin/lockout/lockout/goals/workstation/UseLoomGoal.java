package me.marin.lockout.lockout.goals.workstation;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class UseLoomGoal extends Goal {

    public UseLoomGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Loom to design a Banner";
    }

    private static final ItemStack ITEM_STACK = Items.LOOM.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }
}
