package me.marin.lockout.lockout.goals.spyglass.unique;

import me.marin.lockout.lockout.interfaces.LookAtUniqueMobsGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class LookAt5UniqueMobsGoal extends LookAtUniqueMobsGoal {

    private final ItemStack ITEM = Items.SPYGLASS.getDefaultStack();

    public LookAt5UniqueMobsGoal(String id, String data) {
        super(id, data);
        ITEM.setCount(getAmount());
    }

    @Override
    public String getGoalName() {
        return "Look at 5 Unique Mobs with Spyglass";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }

    @Override
    public int getAmount() {
        return 5;
    }

}
