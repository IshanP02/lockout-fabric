package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public abstract class NameMobGoal extends Goal {

    public NameMobGoal(String id, String data) {
        super(id, data);
    }

    public abstract EntityType<?> getEntityType();

    public abstract String getRequiredName();

    public boolean matchesEntity(LivingEntity entity) {
        return entity.getType().equals(getEntityType());
    }
}
