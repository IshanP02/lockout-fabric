package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class OpponentHitByEggGoal extends Goal {

    public OpponentHitByEggGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Opponent hit by Egg";
    }

    private static final ItemStack ITEM_STACK = Items.EGG.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}