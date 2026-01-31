package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.lockout.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class OpponentHitByWindChargeGoal extends Goal {

    public OpponentHitByWindChargeGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Opponent hit by Wind Charge";
    }

    private static final ItemStack ITEM_STACK = Items.WIND_CHARGE.getDefaultStack();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}