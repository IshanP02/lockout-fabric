package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LockPickBanSelectionsPayload(
        List<String> pendingPicks,
        List<String> pendingBans,
        Map<String, String> goalToPlayerMap
) implements CustomPayload {
    public static final Id<LockPickBanSelectionsPayload> ID = new Id<>(Constants.LOCK_PICK_BAN_SELECTIONS_PACKET);
    public static final PacketCodec<RegistryByteBuf, LockPickBanSelectionsPayload> CODEC = new PacketCodec<>() {
        @Override
        public LockPickBanSelectionsPayload decode(RegistryByteBuf buf) {
            int pendingPicksSize = buf.readInt();
            List<String> pendingPicks = new java.util.ArrayList<>();
            for (int i = 0; i < pendingPicksSize; i++) {
                pendingPicks.add(buf.readString());
            }

            int pendingBansSize = buf.readInt();
            List<String> pendingBans = new java.util.ArrayList<>();
            for (int i = 0; i < pendingBansSize; i++) {
                pendingBans.add(buf.readString());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readString();
                String playerName = buf.readString();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new LockPickBanSelectionsPayload(pendingPicks, pendingBans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryByteBuf buf, LockPickBanSelectionsPayload payload) {
            buf.writeInt(payload.pendingPicks().size());
            for (String pick : payload.pendingPicks()) {
                buf.writeString(pick);
            }

            buf.writeInt(payload.pendingBans().size());
            for (String ban : payload.pendingBans()) {
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
