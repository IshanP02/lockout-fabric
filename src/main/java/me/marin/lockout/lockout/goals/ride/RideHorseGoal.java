package me.marin.lockout.lockout.goals.ride;

import me.marin.lockout.lockout.interfaces.RideEntityGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RideHorseGoal extends RideEntityGoal {

    private static final ItemStack ITEM_STACK = Items.SADDLE.getDefaultInstance();
    public RideHorseGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.HORSE;
    }

    @Override
    public String getGoalName() {
        return "Ride a Horse";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
