package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public abstract class EnterDimensionGoal extends Goal {

    public EnterDimensionGoal(String id, String data) {
        super(id, data);
    }

    public abstract ResourceKey<Level> getWorldRegistryKey();

}
