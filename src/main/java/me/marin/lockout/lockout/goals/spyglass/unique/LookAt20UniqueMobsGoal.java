package me.marin.lockout.lockout.goals.spyglass.unique;

import me.marin.lockout.lockout.interfaces.LookAtUniqueMobsGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class LookAt20UniqueMobsGoal extends LookAtUniqueMobsGoal {

    private final ItemStack ITEM = Items.SPYGLASS.getDefaultStack();

    public LookAt20UniqueMobsGoal(String id, String data) {
        super(id, data);
        ITEM.setCount(getAmount());
    }

    @Override
    public String getGoalName() {
        return "Look at 20 Unique Mobs with Spyglass";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }

    @Override
    public int getAmount() {
        return 20;
    }

}
