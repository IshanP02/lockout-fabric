package me.marin.lockout.lockout.goals.brewing;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BrewLingeringPotionGoal extends Goal {

    private final ItemStack ITEM;
    public BrewLingeringPotionGoal(String id, String data) {
        super(id, data);
        ITEM = Items.LINGERING_POTION.getDefaultInstance();
    }

    @Override
    public String getGoalName() {
        return "Brew a Lingering Potion";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }

}
