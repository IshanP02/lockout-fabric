package me.marin.lockout.lockout.interfaces;

import net.minecraft.item.Item;

public interface FurnaceSmeltTracker {
    void lockout$setLastSmelted(Item item);
    Item lockout$getLastSmelted();
}
