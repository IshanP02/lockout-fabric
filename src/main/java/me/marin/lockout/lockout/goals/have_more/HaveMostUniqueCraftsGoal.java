package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HaveMostUniqueCraftsGoal extends Goal implements CustomTextureRenderer, HasTooltipInfo {

    private static final ItemStack ITEM_STACK = Items.CRAFTING_TABLE.getDefaultStack();
    public HaveMostUniqueCraftsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Craft the most unique Items";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/up_arrow.png");
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawItem(ITEM_STACK, x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Set<Item>> uniqueCrafts = LockoutServer.lockout.uniqueCrafts;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostUniqueCraftsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = uniqueCrafts.getOrDefault(leaderUuid, Set.of()).size();
                tooltip.add(Formatting.GOLD + "Leader: " + Formatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show team members
        for (String playerName : team.getPlayerNames()) {
            ServerPlayerEntity teamPlayer = server.getPlayerManager().getPlayer(playerName);
            if (teamPlayer != null) {
                UUID uuid = teamPlayer.getUuid();
                if (!uuid.equals(leaderUuid)) {
                    int count = uniqueCrafts.getOrDefault(uuid, Set.of()).size();
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
        Map<UUID, Set<Item>> uniqueCrafts = LockoutServer.lockout.uniqueCrafts;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostUniqueCraftsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayerEntity leader = server.getPlayerManager().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = uniqueCrafts.getOrDefault(leaderUuid, Set.of()).size();
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
                        int count = uniqueCrafts.getOrDefault(uuid, Set.of()).size();
                        tooltip.add(team.getColor() + playerName + Formatting.RESET + " - " + count);
                    }
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

}
