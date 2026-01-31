package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class VisitAllSpecificBiomesGoal extends Goal implements Trackable<LockoutTeam, LinkedHashSet<Identifier>>, HasTooltipInfo {

    public VisitAllSpecificBiomesGoal(String id, String data) {
        super(id, data);
    }

    public abstract List<Identifier> getBiomes();

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        var visitedBiomes = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Biomes Visited: " + visitedBiomes.size() + "/" + getBiomes().size());
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(visitedBiomes.stream()
                .map(id -> Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString())
                .toList()));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            var visitedBiomes = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());
            tooltip.add(team.getColor() + team.getDisplayName() + Formatting.RESET + ": " + visitedBiomes.size() + "/" + getBiomes().size());
        }
        tooltip.add(" ");

        return tooltip;
    }

}
