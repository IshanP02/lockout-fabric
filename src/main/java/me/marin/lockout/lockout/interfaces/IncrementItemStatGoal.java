package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import net.minecraft.stat.Stat;
import net.minecraft.item.Item;

import java.util.List;

public abstract class IncrementItemStatGoal extends Goal {

    public IncrementItemStatGoal(String id, String data) {
        super(id, data);
    }

    public abstract List<Stat<Item>> getStats();

}

