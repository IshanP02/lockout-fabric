package me.marin.lockout.lockout.goals.ride;

import me.marin.lockout.lockout.interfaces.RideEntityGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RidePigGoal extends RideEntityGoal {

    private static final ItemStack ITEM_STACK = Items.CARROT_ON_A_STICK.getDefaultInstance();
    public RidePigGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.PIG;
    }

    @Override
    public String getGoalName() {
        return "Ride Pig with Carrot on a Stick";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
