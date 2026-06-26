package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record UpdatePicksBansPayload(List<String> picks, List<String> bans, Map<String, String> goalToPlayerMap) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdatePicksBansPayload> ID = new CustomPacketPayload.Type<>(Constants.UPDATE_PICKS_BANS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePicksBansPayload> CODEC = new StreamCodec<>() {
        @Override
        public UpdatePicksBansPayload decode(RegistryFriendlyByteBuf buf) {
            int picksSize = buf.readInt();
            List<String> picks = new ArrayList<>();
            for (int i = 0; i < picksSize; i++) {
                picks.add(buf.readUtf());
            }

            int bansSize = buf.readInt();
            List<String> bans = new ArrayList<>();
            for (int i = 0; i < bansSize; i++) {
                bans.add(buf.readUtf());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readUtf();
                String playerName = buf.readUtf();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new UpdatePicksBansPayload(picks, bans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UpdatePicksBansPayload payload) {
            buf.writeInt(payload.picks().size());
            for (String pick : payload.picks()) {
                buf.writeUtf(pick);
            }

            buf.writeInt(payload.bans().size());
            for (String ban : payload.bans()) {
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
