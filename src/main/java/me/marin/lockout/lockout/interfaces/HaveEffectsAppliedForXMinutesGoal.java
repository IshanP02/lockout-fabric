package me.marin.lockout.lockout.interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.Utility;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

public abstract class HaveEffectsAppliedForXMinutesGoal extends Goal implements HasTooltipInfo {

    public HaveEffectsAppliedForXMinutesGoal(String id, String data) {
        super(id, data);
    }

    public abstract int getMinutes();

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        long timeWithEffects = Math.min(getMinutes() * 60 * 20, LockoutServer.lockout.appliedEffectsTime.getOrDefault(player.getUuid(), 0L));
        LockoutTeamServer serverTeam = ((LockoutTeamServer) team);

        tooltip.add(" ");
        tooltip.add("Time with Effects Applied: " + Utility.ticksToTimer(timeWithEffects));
        if (serverTeam.getPlayers().size() > 1) {
            tooltip.add(" ");
            for (UUID uuid : ((LockoutTeamServer) team).getPlayers()) {
                if (!Objects.equals(uuid, player.getUuid())) {
                    tooltip.add(serverTeam.getPlayerName(uuid) + ": " + Utility.ticksToTimer(timeWithEffects));
                }
            }
        }
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            for (UUID uuid : ((LockoutTeamServer) team).getPlayers()) {
                long timeWithEffects = Math.min(getMinutes() * 60 * 20, LockoutServer.lockout.appliedEffectsTime.getOrDefault(uuid, 0L));
                tooltip.add(team.getColor() + ((LockoutTeamServer) team).getPlayerName(uuid) + Formatting.RESET + ": " + Utility.ticksToTimer(timeWithEffects));
            }
        }
        tooltip.add(" ");

        return tooltip;
    }

}
