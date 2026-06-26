package me.marin.lockout.lockout.goals.consume;

import me.marin.lockout.lockout.interfaces.DrinkPotionGoal;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.Holder;

public class DrinkWaterBottleGoal extends DrinkPotionGoal {

    public DrinkWaterBottleGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public Holder<Potion> getPotion() {
        return Potions.WATER;
    }

    @Override
    public String getGoalName() {
        return "Drink Water Bottle";
    }



}
