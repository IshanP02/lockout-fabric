package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FillShelfWithShelvesGoal extends Goal {

    private static final Item ITEM = Items.OAK_SHELF;

    public FillShelfWithShelvesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Fill a Shelf with Shelves";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultStack();
    }

}
