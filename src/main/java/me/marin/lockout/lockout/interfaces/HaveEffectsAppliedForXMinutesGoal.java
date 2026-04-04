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
        LockoutTeamServer serverTeam = ((LockoutTeamServer) team);
        int requiredTicks = getMinutes() * 60 * 20;

        // Calculate total team time
        long totalTeamTime = 0;
        for (UUID uuid : serverTeam.getPlayers()) {
            totalTeamTime += LockoutServer.lockout.appliedEffectsTime.getOrDefault(uuid, 0L);
        }
        totalTeamTime = Math.min(requiredTicks, totalTeamTime);

        tooltip.add(" ");
        tooltip.add("Total Team Time: " + Utility.ticksToTimer(totalTeamTime));
        tooltip.add(" ");
        
        // Show player's time first
        long playerTime = Math.min(requiredTicks, LockoutServer.lockout.appliedEffectsTime.getOrDefault(player.getUuid(), 0L));
        tooltip.add("You: " + Utility.ticksToTimer(playerTime));
        
        // Then show teammates' times
        for (UUID uuid : serverTeam.getPlayers()) {
            if (!Objects.equals(uuid, player.getUuid())) {
                long timeWithEffects = Math.min(requiredTicks, LockoutServer.lockout.appliedEffectsTime.getOrDefault(uuid, 0L));
                tooltip.add(serverTeam.getPlayerName(uuid) + ": " + Utility.ticksToTimer(timeWithEffects));
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
