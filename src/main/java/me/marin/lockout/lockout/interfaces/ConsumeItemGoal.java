package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class ConsumeItemGoal extends Goal {

    public ConsumeItemGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public ItemStack getTextureItemStack() {
        return getItem().getDefaultInstance();
    }

    public abstract Item getItem();

}
