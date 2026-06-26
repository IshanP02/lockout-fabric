package me.marin.lockout.lockout.goals.workstation;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class UseAnvilGoal extends Goal {

    public UseAnvilGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Anvil";
    }

    private static final ItemStack ITEM_STACK = Items.ANVIL.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
