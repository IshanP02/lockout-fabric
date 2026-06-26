package me.marin.lockout.lockout.goals.consume;

import me.marin.lockout.lockout.interfaces.ConsumeItemGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class EatPoisonousPotatoGoal extends ConsumeItemGoal {

    public EatPoisonousPotatoGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Eat a Poisonous Potato";
    }

    @Override
    public Item getItem() {
        return Items.POISONOUS_POTATO;
    }

}
