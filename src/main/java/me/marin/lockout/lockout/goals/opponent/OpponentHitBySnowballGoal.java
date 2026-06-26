package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class OpponentHitBySnowballGoal extends Goal {

    public OpponentHitBySnowballGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Opponent hit by Snowball";
    }

    private static final ItemStack ITEM_STACK = Items.SNOWBALL.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}