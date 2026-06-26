package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HaveMostAdvancementsGoal extends Goal implements TextureProvider, HasTooltipInfo {

    public HaveMostAdvancementsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have the most Advancements";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/more_advancements.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> playerAdvancements = LockoutServer.lockout.playerAdvancements;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostAdvancementsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayer leader = server.getPlayerList().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = playerAdvancements.getOrDefault(leaderUuid, 0);
                tooltip.add(ChatFormatting.GOLD + "Leader: " + ChatFormatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show team members
        for (String playerName : team.getPlayerNames()) {
            ServerPlayer teamPlayer = server.getPlayerList().getPlayer(playerName);
            if (teamPlayer != null) {
                UUID uuid = teamPlayer.getUUID();
                if (!uuid.equals(leaderUuid)) {
                    int count = playerAdvancements.getOrDefault(uuid, 0);
                    tooltip.add(ChatFormatting.GRAY + playerName + " - " + count);
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Integer> playerAdvancements = LockoutServer.lockout.playerAdvancements;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostAdvancementsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayer leader = server.getPlayerList().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = playerAdvancements.getOrDefault(leaderUuid, 0);
                tooltip.add(ChatFormatting.GOLD + "Leader: " + ChatFormatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show all players by team
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            for (String playerName : team.getPlayerNames()) {
                ServerPlayer teamPlayer = server.getPlayerList().getPlayer(playerName);
                if (teamPlayer != null) {
                    UUID uuid = teamPlayer.getUUID();
                    if (!uuid.equals(leaderUuid)) {
                        int count = playerAdvancements.getOrDefault(uuid, 0);
                        tooltip.add(team.getColor() + playerName + ChatFormatting.RESET + " - " + count);
                    }
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

}