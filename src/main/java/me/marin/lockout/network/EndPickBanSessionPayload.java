package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.*;

public record EndPickBanSessionPayload(
        boolean cancelled,
        Set<String> finalPicks,
        Set<String> finalBans,
        Map<String, String> goalToPlayerMap
) implements CustomPayload {
    public static final Id<EndPickBanSessionPayload> ID = new Id<>(Constants.END_PICK_BAN_SESSION_PACKET);
    public static final PacketCodec<RegistryByteBuf, EndPickBanSessionPayload> CODEC = new PacketCodec<>() {
        @Override
        public EndPickBanSessionPayload decode(RegistryByteBuf buf) {
            boolean cancelled = buf.readBoolean();

            int picksSize = buf.readInt();
            Set<String> finalPicks = new HashSet<>();
            for (int i = 0; i < picksSize; i++) {
                finalPicks.add(buf.readString());
            }

            int bansSize = buf.readInt();
            Set<String> finalBans = new HashSet<>();
            for (int i = 0; i < bansSize; i++) {
                finalBans.add(buf.readString());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readString();
                String playerName = buf.readString();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new EndPickBanSessionPayload(cancelled, finalPicks, finalBans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryByteBuf buf, EndPickBanSessionPayload payload) {
            buf.writeBoolean(payload.cancelled());

            buf.writeInt(payload.finalPicks().size());
            for (String pick : payload.finalPicks()) {
                buf.writeString(pick);
            }

            buf.writeInt(payload.finalBans().size());
            for (String ban : payload.finalBans()) {
                buf.writeString(ban);
            }

            buf.writeInt(payload.goalToPlayerMap().size());
            for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                buf.writeString(entry.getKey());
                buf.writeString(entry.getValue());
            }
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
