package me.marin.lockout;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {

    public enum Type {
        LOCAL,
        TEAM
    }

    private static final Map<UUID, Type> map = new HashMap<>();

    public static void setChat(Player player, Type type) {
        map.put(player.getUUID(), type);
    }

    public static Type getChat(Player player) {
        return map.getOrDefault(player.getUUID(), Type.LOCAL);
    }

}
