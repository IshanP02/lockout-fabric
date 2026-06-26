package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdatePickBanSessionPayload> ID = new CustomPacketPayload.Type<>(Constants.UPDATE_PICK_BAN_SESSION_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePickBanSessionPayload> CODEC = new StreamCodec<>() {
        @Override
        public UpdatePickBanSessionPayload decode(RegistryFriendlyByteBuf buf) {
            int currentRound = buf.readInt();
            boolean isTeam1Turn = buf.readBoolean();
            String team1Name = buf.readUtf();
            String team2Name = buf.readUtf();

            int lockedPicksSize = buf.readInt();
            Set<String> allLockedPicks = new HashSet<>();
            for (int i = 0; i < lockedPicksSize; i++) {
                allLockedPicks.add(buf.readUtf());
            }

            int lockedBansSize = buf.readInt();
            Set<String> allLockedBans = new HashSet<>();
            for (int i = 0; i < lockedBansSize; i++) {
                allLockedBans.add(buf.readUtf());
            }

            int pendingPicksSize = buf.readInt();
            List<String> pendingPicks = new ArrayList<>();
            for (int i = 0; i < pendingPicksSize; i++) {
                pendingPicks.add(buf.readUtf());
            }

            int pendingBansSize = buf.readInt();
            List<String> pendingBans = new ArrayList<>();
            for (int i = 0; i < pendingBansSize; i++) {
                pendingBans.add(buf.readUtf());
            }

            int selectionLimit = buf.readInt();

            int goalToPlayerMapSize = buf.readInt();
            Map<String, String> goalToPlayerMap = new HashMap<>();
            for (int i = 0; i < goalToPlayerMapSize; i++) {
                String goalId = buf.readUtf();
                String playerName = buf.readUtf();
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
        public void encode(RegistryFriendlyByteBuf buf, UpdatePickBanSessionPayload payload) {
            buf.writeInt(payload.currentRound());
            buf.writeBoolean(payload.isTeam1Turn());
            buf.writeUtf(payload.team1Name());
            buf.writeUtf(payload.team2Name());

            buf.writeInt(payload.allLockedPicks().size());
            for (String pick : payload.allLockedPicks()) {
                buf.writeUtf(pick);
            }

            buf.writeInt(payload.allLockedBans().size());
            for (String ban : payload.allLockedBans()) {
                buf.writeUtf(ban);
            }

            buf.writeInt(payload.pendingPicks().size());
            for (String pick : payload.pendingPicks()) {
                buf.writeUtf(pick);
            }

            buf.writeInt(payload.pendingBans().size());
            for (String ban : payload.pendingBans()) {
                buf.writeUtf(ban);
            }

            buf.writeInt(payload.selectionLimit());

            buf.writeInt(payload.goalToPlayerMap().size());
            for (Map.Entry<String, String> entry : payload.goalToPlayerMap().entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeUtf(entry.getValue());
            }

            buf.writeInt(payload.maxRounds());
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
