package me.marin.lockout.network;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.ChatFormatting;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public record LockoutGoalsTeamsPayload(List<LockoutTeam> teams, List<Pair<Pair<String, String>, Integer>> goals,
                                       boolean isRunning) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LockoutGoalsTeamsPayload> ID = new CustomPacketPayload.Type<>(Constants.LOCKOUT_GOALS_TEAMS_PACKET);

    public static final StreamCodec<RegistryFriendlyByteBuf, LockoutGoalsTeamsPayload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, LockoutGoalsTeamsPayload>() {
        @Override
        public LockoutGoalsTeamsPayload decode(RegistryFriendlyByteBuf buf) {
            // Read teams
            int teamsSize = buf.readInt();
            List<LockoutTeam> teams = new ArrayList<>(teamsSize);
            for (int i = 0; i < teamsSize; i++) {
                int teamSize = buf.readInt();
                ChatFormatting color = ChatFormatting.valueOf(buf.readUtf().toUpperCase());
                List<String> playerNames = new ArrayList<>();
                for (int j = 0; j < teamSize; j++) {
                    String playerName = buf.readUtf();
                    playerNames.add(playerName);
                }
                teams.add(new LockoutTeam(playerNames, color));
            }

            // Read goals
            int size = buf.readInt();
            List<Pair<Pair<String, String>, Integer>> goals = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                goals.add(new Pair<>(new Pair<>(buf.readUtf(), buf.readUtf()), buf.readInt()));
            }

            boolean isRunning = buf.readBoolean();
            return new LockoutGoalsTeamsPayload(teams, goals, isRunning);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LockoutGoalsTeamsPayload payload) {
            // Write teams
            List<LockoutTeam> teams = payload.teams();
            buf.writeInt(teams.size());
            for (LockoutTeam team : payload.teams()) {
                buf.writeInt(team.getPlayerNames().size());
                buf.writeUtf(team.getColor().name().toLowerCase());
                for (String playerName : team.getPlayerNames()) {
                    buf.writeUtf(playerName);
                }
            }

            // Write goals
            buf.writeInt(payload.goals().size());
            for (Pair<Pair<String, String>, Integer> goal : payload.goals()) {
                buf.writeUtf(goal.getA().getA());
                buf.writeUtf(goal.getA().getB());
                buf.writeInt(goal.getB());
            }

            buf.writeBoolean(payload.isRunning);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
