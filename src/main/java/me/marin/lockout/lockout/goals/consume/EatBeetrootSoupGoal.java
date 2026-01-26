package me.marin.lockout.lockout.goals.consume;

import me.marin.lockout.lockout.interfaces.ConsumeItemGoal;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class EatBeetrootSoupGoal extends ConsumeItemGoal {

    public EatBeetrootSoupGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Eat Beetroot Soup";
    }

    @Override
    public Item getItem() {
        return Items.BEETROOT_SOUP;
    }

}
