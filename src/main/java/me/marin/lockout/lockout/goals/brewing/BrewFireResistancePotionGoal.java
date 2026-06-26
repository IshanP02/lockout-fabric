package me.marin.lockout.lockout.goals.brewing;

import me.marin.lockout.lockout.interfaces.ObtainPotionItemGoal;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.Holder;

import java.util.List;

public class BrewFireResistancePotionGoal extends ObtainPotionItemGoal {

    public BrewFireResistancePotionGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Holder<Potion>> POTION_LIST = List.of(Potions.FIRE_RESISTANCE, Potions.LONG_FIRE_RESISTANCE);
    @Override
    public List<Holder<Potion>> getPotions() {
        return POTION_LIST;
    }

    @Override
    public String getGoalName() {
        return "Brew a Potion of Fire Resistance";
    }

}
