package me.marin.lockout.lockout.goals.dimension;

import me.marin.lockout.lockout.interfaces.EnterDimensionGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class EnterEndGoal extends EnterDimensionGoal {

    private static final Item ITEM = Items.END_PORTAL_FRAME;

    public EnterEndGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Enter The End";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

    @Override
    public ResourceKey<Level> getWorldRegistryKey() {
        return Level.END;
    }


}
