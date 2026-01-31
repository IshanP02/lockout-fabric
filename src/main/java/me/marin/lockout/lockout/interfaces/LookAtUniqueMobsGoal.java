package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class LookAtUniqueMobsGoal extends Goal implements RequiresAmount, HasTooltipInfo {

    public LookAtUniqueMobsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        var mobs = LockoutServer.lockout.lookedAtMobTypes.getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Unique Mobs: " + LockoutServer.lockout.lookedAtMobTypes.getOrDefault(team, new LinkedHashSet<>()).size() + "/" + getAmount());
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(mobs.stream().map(type -> type.getName().getString()).toList()));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            var mobs = LockoutServer.lockout.lookedAtMobTypes.getOrDefault(team, new LinkedHashSet<>());
            tooltip.add(team.getColor() + team.getDisplayName() + Formatting.RESET + ": " + mobs.size() + "/" + getAmount());
        }
        tooltip.add(" ");

        return tooltip;
    }

}
