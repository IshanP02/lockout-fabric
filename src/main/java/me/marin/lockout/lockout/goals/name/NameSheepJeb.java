package me.marin.lockout.lockout.goals.name;

import me.marin.lockout.lockout.interfaces.NameMobGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class NameSheepJeb extends NameMobGoal {

    public NameSheepJeb(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.SHEEP;
    }

    @Override
    public String getRequiredName() {
        return "jeb_";
    }

    @Override
    public String getGoalName() {
        return "Name a Sheep \"jeb_\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return Items.NAME_TAG.getDefaultInstance();
    }
}
