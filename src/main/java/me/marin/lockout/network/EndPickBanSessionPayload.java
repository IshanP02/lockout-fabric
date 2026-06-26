package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.*;

public record EndPickBanSessionPayload(
        boolean cancelled,
        Set<String> finalPicks,
        Set<String> finalBans,
        Map<String, String> goalToPlayerMap
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EndPickBanSessionPayload> ID = new CustomPacketPayload.Type<>(Constants.END_PICK_BAN_SESSION_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, EndPickBanSessionPayload> CODEC = new StreamCodec<>() {
        @Override
        public EndPickBanSessionPayload decode(RegistryFriendlyByteBuf buf) {
            boolean cancelled = buf.readBoolean();

            int picksSize = buf.readInt();
            Set<String> finalPicks = new HashSet<>();
            for (int i = 0; i < picksSize; i++) {
                finalPicks.add(buf.readUtf());
            }

            int bansSize = buf.readInt();
            Set<String> finalBans = new HashSet<>();
            for (int i = 0; i < bansSize; i++) {
                finalBans.add(buf.readUtf());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readUtf();
                String playerName = buf.readUtf();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new EndPickBanSessionPayload(cancelled, finalPicks, finalBans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EndPickBanSessionPayload payload) {
            buf.writeBoolean(payload.cancelled());

            buf.writeInt(payload.finalPicks().size());
            for (String pick : payload.finalPicks()) {
                buf.writeUtf(pick);
            }

            buf.writeInt(payload.finalBans().size());
            for (String ban : payload.finalBans()) {
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
