package me.marin.lockout.lockout.goals.ride;

import me.marin.lockout.lockout.interfaces.RideEntityGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RideMinecartGoal extends RideEntityGoal {

    private static final ItemStack ITEM_STACK = Items.MINECART.getDefaultInstance();
    public RideMinecartGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.MINECART;
    }

    @Override
    public String getGoalName() {
        return "Ride a Minecart";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
