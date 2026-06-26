package me.marin.lockout;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.players.PlayerList;

import java.util.*;

public class CompassItemHandler {

    public static boolean isCompass(ItemStack item) {
        if (item == null) return false;
        if (item.getItem() != Items.COMPASS) return false;
        var customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        // New CustomData API differs across mappings; use string inspection as a stable fallback
        return customData.toString().contains("PlayerTracker");
    }

    public final List<UUID> players = new ArrayList<>();
    public final Map<UUID, String> playerNames = new HashMap<>();
    public final Map<UUID, Integer> currentSelection = new HashMap<>();
    public final Map<UUID, Integer> compassSlots = new HashMap<>();

    public CompassItemHandler(List<UUID> players, PlayerList playerManager) {
        for (int i = 0; i < players.size(); i++) {
            UUID playerId = players.get(i);
            this.players.add(playerId);
            this.playerNames.put(playerId, playerManager.getPlayer(playerId).getName().getString());

            this.currentSelection.put(playerId, i == 0 ? 1 : 0);
        }
    }

    public void cycle(Player player) {
        int cur = currentSelection.get(player.getUUID());
        int next = (cur + 1) % players.size();
        if (players.get(next).equals(player.getUUID())) {
            next = (next + 1) % players.size();
        }
        currentSelection.put(player.getUUID(), next);
    }

    public ItemStack newCompass() {
        ItemStack compass = Items.COMPASS.getDefaultInstance();
        CompoundTag compound = new CompoundTag();
        compound.putString("PlayerTracker", UUID.randomUUID().toString());
        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
        return compass;
    }

}
