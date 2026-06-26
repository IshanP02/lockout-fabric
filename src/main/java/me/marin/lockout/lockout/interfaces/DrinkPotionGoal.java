package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;

public abstract class DrinkPotionGoal extends Goal {

    private final ItemStack displayItem;
    public DrinkPotionGoal(String id, String data) {
        super(id, data);
        displayItem = PotionContents.createItemStack(Items.POTION, getPotion());
    }

    @Override
    public ItemStack getTextureItemStack() {
        return displayItem;
    }

    public abstract Holder<Potion> getPotion();

}
