package me.marin.lockout.lockout.goals.status_effect;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RemoveStatusEffectUsingMilkGoal extends Goal {

    private static final ItemStack ITEM_STACK = Items.MILK_BUCKET.getDefaultInstance();

    public RemoveStatusEffectUsingMilkGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Remove Effect using Milk Bucket";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
