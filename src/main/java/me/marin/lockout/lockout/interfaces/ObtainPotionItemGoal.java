package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;

import java.util.List;

public abstract class ObtainPotionItemGoal extends Goal {

    private final ItemStack ITEM;
    public ObtainPotionItemGoal(String id, String data) {
        super(id, data);
        ITEM = PotionContents.createItemStack(Items.POTION, getPotions().getFirst());
    }

    public abstract List<Holder<Potion>> getPotions();

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }
}
