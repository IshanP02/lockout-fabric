package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HaveMostXPLevelsGoal extends Goal implements TextureProvider, HasTooltipInfo {

    public HaveMostXPLevelsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most XP Levels";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/more_level.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> levels = LockoutServer.lockout.levels;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader across all teams
        UUID leaderUuid = LockoutServer.lockout.mostLevelsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderLevels = levels.getOrDefault(leaderUuid, 0);
                tooltip.add(Formatting.GOLD + "Leader: " + Formatting.RESET + leader.getName().getString() + " - " + leaderLevels);
            }
        }
        
        // Show team members' progress
        for (String playerName : team.getPlayerNames()) {
            ServerPlayerEntity teamPlayer = server.getPlayerManager().getPlayer(playerName);
            if (teamPlayer != null) {
                UUID uuid = teamPlayer.getUuid();
                // Skip if this player is already shown as leader
                if (!uuid.equals(leaderUuid)) {
                    int playerLevels = levels.getOrDefault(uuid, 0);
                    tooltip.add(Formatting.GRAY + playerName + " - " + playerLevels);
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> levels = LockoutServer.lockout.levels;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostLevelsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderLevels = levels.getOrDefault(leaderUuid, 0);
                tooltip.add(Formatting.GOLD + "Leader: " + Formatting.RESET + leader.getName().getString() + " - " + leaderLevels);
            }
        }
        
        // Show all teams
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                ServerPlayerEntity teamPlayer = server.getPlayerManager().getPlayer(playerName);
                if (teamPlayer != null) {
                    UUID uuid = teamPlayer.getUuid();
                    // Skip if already shown as leader
                    if (!uuid.equals(leaderUuid)) {
                        int playerLevels = levels.getOrDefault(uuid, 0);
                        tooltip.add(team.getColor() + playerName + Formatting.RESET + " - " + playerLevels);
                    }
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

}
