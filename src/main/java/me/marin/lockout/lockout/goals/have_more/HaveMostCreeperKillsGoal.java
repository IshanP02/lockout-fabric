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

public class HaveMostCreeperKillsGoal extends Goal implements TextureProvider, HasTooltipInfo {

    public HaveMostCreeperKillsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most Creeper Kills";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/more_creeper_kills.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> creeperKills = LockoutServer.lockout.creeperKills;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostCreeperKillsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = creeperKills.getOrDefault(leaderUuid, 0);
                tooltip.add(Formatting.GOLD + "Leader: " + Formatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show team members
        for (String playerName : team.getPlayerNames()) {
            ServerPlayerEntity teamPlayer = server.getPlayerManager().getPlayer(playerName);
            if (teamPlayer != null) {
                UUID uuid = teamPlayer.getUuid();
                if (!uuid.equals(leaderUuid)) {
                    int count = creeperKills.getOrDefault(uuid, 0);
                    tooltip.add(Formatting.GRAY + playerName + " - " + count);
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> creeperKills = LockoutServer.lockout.creeperKills;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostCreeperKillsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = creeperKills.getOrDefault(leaderUuid, 0);
                tooltip.add(Formatting.GOLD + "Leader: " + Formatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show all players by team
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                ServerPlayerEntity teamPlayer = server.getPlayerManager().getPlayer(playerName);
                if (teamPlayer != null) {
                    UUID uuid = teamPlayer.getUuid();
                    if (!uuid.equals(leaderUuid)) {
                        int count = creeperKills.getOrDefault(uuid, 0);
                        tooltip.add(team.getColor() + playerName + Formatting.RESET + " - " + count);
                    }
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

}