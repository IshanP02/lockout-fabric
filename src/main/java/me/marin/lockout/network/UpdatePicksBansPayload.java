package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record UpdatePicksBansPayload(List<String> picks, List<String> bans, Map<String, String> goalToPlayerMap) implements CustomPayload {
    public static final Id<UpdatePicksBansPayload> ID = new Id<>(Constants.UPDATE_PICKS_BANS_PACKET);
    public static final PacketCodec<RegistryByteBuf, UpdatePicksBansPayload> CODEC = new PacketCodec<>() {
        @Override
        public UpdatePicksBansPayload decode(RegistryByteBuf buf) {
            int picksSize = buf.readInt();
            List<String> picks = new ArrayList<>();
            for (int i = 0; i < picksSize; i++) {
                picks.add(buf.readString());
            }

            int bansSize = buf.readInt();
            List<String> bans = new ArrayList<>();
            for (int i = 0; i < bansSize; i++) {
                bans.add(buf.readString());
            }

            int mapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String goalId = buf.readString();
                String playerName = buf.readString();
                goalToPlayerMap.put(goalId, playerName);
            }

            return new UpdatePicksBansPayload(picks, bans, goalToPlayerMap);
        }

        @Override
        public void encode(RegistryByteBuf buf, UpdatePicksBansPayload payload) {
            buf.writeInt(payload.picks().size());
            for (String pick : payload.picks()) {
                buf.writeString(pick);
            }

            buf.writeInt(payload.bans().size());
            for (String ban : payload.bans()) {
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
