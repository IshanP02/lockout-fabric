package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public abstract class LeashMobGoal extends Goal {

    private final EntityType<?> targetMobType;

    public LeashMobGoal(String id, String data, EntityType<?> targetMobType) {
        super(id, data);
        this.targetMobType = targetMobType;
    }

    public boolean matchesMob(Entity entity) {
        return entity.getType() == targetMobType;
    }

    public EntityType<?> getTargetMobType() {
        return targetMobType;
    }
}
