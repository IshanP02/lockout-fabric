package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record UpdatePickBanSessionPayload(
        int currentRound,
        boolean isTeam1Turn,
        String team1Name,
        String team2Name,
        Set<String> allLockedPicks,
        Set<String> allLockedBans,
        List<String> pendingPicks,
        List<String> pendingBans,
        int selectionLimit,
        Map<String, String> goalToPlayerMap,
        int maxRounds
) implements CustomPayload {
    public static final Id<UpdatePickBanSessionPayload> ID = new Id<>(Constants.UPDATE_PICK_BAN_SESSION_PACKET);
    public static final PacketCodec<RegistryByteBuf, UpdatePickBanSessionPayload> CODEC = new PacketCodec<>() {
        @Override
        public UpdatePickBanSessionPayload decode(RegistryByteBuf buf) {
            int currentRound = buf.readInt();
            boolean isTeam1Turn = buf.readBoolean();
            String team1Name = buf.readString();
            String team2Name = buf.readString();

            int lockedPicksSize = buf.readInt();
            Set<String> allLockedPicks = new HashSet<>();
            for (int i = 0; i < lockedPicksSize; i++) {
                allLockedPicks.add(buf.readString());
            }

            int lockedBansSize = buf.readInt();
            Set<String> allLockedBans = new HashSet<>();
            for (int i = 0; i < lockedBansSize; i++) {
                allLockedBans.add(buf.readString());
            }

            int pendingPicksSize = buf.readInt();
            List<String> pendingPicks = new ArrayList<>();
            for (int i = 0; i < pendingPicksSize; i++) {
                pendingPicks.add(buf.readString());
            }

            int pendingBansSize = buf.readInt();
            List<String> pendingBans = new ArrayList<>();
            for (int i = 0; i < pendingBansSize; i++) {
                pendingBans.add(buf.readString());
            }

            int selectionLimit = buf.readInt();

            int goalToPlayerMapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < goalToPlayerMapSize; i++) {
                String goalId = buf.readString();
                String playerName = buf.readString();
                goalToPlayerMap.put(goalId, playerName);
            }

            int maxRounds = buf.readInt();

            return new UpdatePickBanSessionPayload(
                    currentRound, isTeam1Turn, team1Name, team2Name,
                    allLockedPicks, allLockedBans, pendingPicks, pendingBans,
                    selectionLimit, goalToPlayerMap, maxRounds
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, UpdatePickBanSessionPayload payload) {
            buf.writeInt(payload.currentRound());
            buf.writeBoolean(payload.isTeam1Turn());
            buf.writeString(payload.team1Name());
            buf.writeString(payload.team2Name());

            buf.writeInt(payload.allLockedPicks().size());
            for (String pick : payload.allLockedPicks()) {
                buf.writeString(pick);
            }

            buf.writeInt(payload.allLockedBans().size());
            for (String ban : payload.allLockedBans()) {
                buf.writeString(ban);
            }

            buf.writeInt(payload.pendingPicks().size());
            for (String pick : payload.pendingPicks()) {
                buf.writeString(pick);
            }

            buf.writeInt(payload.pendingBans().size());
            for (String ban : payload.pendingBans()) {
                buf.writeString(ban);
            }

            buf.writeInt(payload.selectionLimit());

            buf.writeInt(payload.goalToPlayerMap().size());
            for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                buf.writeString(entry.getKey());
                buf.writeString(entry.getValue());
            }

            buf.writeInt(payload.maxRounds());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
