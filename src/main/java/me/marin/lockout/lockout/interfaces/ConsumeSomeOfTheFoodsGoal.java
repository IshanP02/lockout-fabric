package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public abstract class ConsumeSomeOfTheFoodsGoal extends Goal implements RequiresAmount, CycleItemTexturesProvider {

    public ConsumeSomeOfTheFoodsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public ItemStack getTextureItemStack() {
        return getItems().get(0).getDefaultInstance();
    }

    public abstract List<Item> getItems();

    @Override
    public List<Item> getItemsToDisplay() {
        return getItems();
    }

}