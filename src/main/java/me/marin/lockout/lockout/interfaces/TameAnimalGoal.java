package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.entity.EntityType;

public abstract class TameAnimalGoal extends Goal {

    public TameAnimalGoal(String id, String data) {
        super(id, data);
    }

    public abstract EntityType<?> getAnimal();

    /**
     * Check if the given entity type matches this goal.
     * By default, checks if it equals getAnimal().
     * Override to support multiple entity types.
     */
    public boolean matchesAnimal(EntityType<?> entityType) {
        return entityType.equals(getAnimal());
    }

}
