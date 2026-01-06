package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;

public abstract class LeashUniqueEntitiesAtOnceGoal extends Goal implements RequiresAmount {

    private final ItemStack DISPLAY_ITEM_STACK = Items.LEAD.getDefaultStack();

    public LeashUniqueEntitiesAtOnceGoal(String id, String data) {
        super(id, data);
        DISPLAY_ITEM_STACK.setCount(getAmount());
    }

    public int getRequiredUniqueTypes() {
        return getAmount();
    }

    @Override
    public String getGoalName() {
        return String.format("Attach Lead to %d Unique Entities at Once", getAmount());
    }

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

    // Static tracking methods for mixin to use
    public static void addLeashedType(UUID playerUuid, EntityType<?> entityType) {
        LockoutServer.lockout.leashedEntities.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(entityType);
    }

    public static void removeLeashedType(UUID playerUuid, EntityType<?> entityType) {
        Set<EntityType<?>> types = LockoutServer.lockout.leashedEntities.get(playerUuid);
        if (types != null) {
            types.remove(entityType);
            if (types.isEmpty()) {
                LockoutServer.lockout.leashedEntities.remove(playerUuid);
            }
        }
    }

    public static int getUniqueLeashedCount(UUID playerUuid) {
        Set<EntityType<?>> types = LockoutServer.lockout.leashedEntities.get(playerUuid);
        return types != null ? types.size() : 0;
    }

    public static void clearTracking() {
        LockoutServer.lockout.leashedEntities.clear();
    }

}
