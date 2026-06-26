package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LockPickBanSelectionsPayload(
        List<String> pendingPicks,
        List<String> pendingBans,
        Map<String, String> goalToPlayerMap
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LockPickBanSelectionsPayload> ID = new CustomPacketPayload.Type<>(Constants.LOCK_PICK_BAN_SELECTIONS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, LockPickBanSelectionsPayload> CODEC = new StreamCodec<>() {
        @Override
        public LockPickBanSelectionsPayload decode(RegistryFriendlyByteBuf buf) {
            int pendingPicksSize = buf.readInt();
            List<String> pendingPicks = new java.util.ArrayList<>();
            for (int i = 0; i < pendingPicksSize; i++) {
                pendingPicks.add(buf.readUtf());
            }

            int pendingBansSize = buf.readInt();
            List<String> pendingBans = new java.util.ArrayList<>();
            for (int i = 0; i < pendingBansSize; i++) {
                pendingBans.add(buf.readUtf());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readUtf();
                String playerName = buf.readUtf();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new LockPickBanSelectionsPayload(pendingPicks, pendingBans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LockPickBanSelectionsPayload payload) {
            buf.writeInt(payload.pendingPicks().size());
            for (String pick : payload.pendingPicks()) {
                buf.writeUtf(pick);
            }

            buf.writeInt(payload.pendingBans().size());
            for (String ban : payload.pendingBans()) {
                buf.writeUtf(ban);
            }

            buf.writeInt(payload.goalToPlayerMap().size());
            for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeUtf(entry.getValue());
            }
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
