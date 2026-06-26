package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FillDecoratedPotGoal extends Goal {

    private static final Item ITEM = Items.DECORATED_POT;

    public FillDecoratedPotGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Fill a Decorated Pot";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

}
