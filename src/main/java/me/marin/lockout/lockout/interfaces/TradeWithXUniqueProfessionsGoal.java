package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class TradeWithXUniqueProfessionsGoal extends Goal implements RequiresAmount, Trackable<LockoutTeam, LinkedHashSet<VillagerProfession>>, HasTooltipInfo {

    public TradeWithXUniqueProfessionsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            LinkedHashSet<VillagerProfession> professions = getTrackerMap().get(team);
            int count = professions != null ? professions.size() : 0;
            tooltip.add(team.getColor() + team.getDisplayName() + ChatFormatting.RESET + ": " + count + "/" + getAmount());
        }
        tooltip.add(" ");

        return tooltip;
    }

}
