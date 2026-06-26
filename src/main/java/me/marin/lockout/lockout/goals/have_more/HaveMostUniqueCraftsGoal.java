package me.marin.lockout.lockout.goals.have_more;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HaveMostUniqueCraftsGoal extends Goal implements CustomTextureRenderer, HasTooltipInfo {

    private static final ItemStack ITEM_STACK = Items.CRAFTING_TABLE.getDefaultInstance();
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

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/up_arrow.png");
    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.item(ITEM_STACK, x, y);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        Map<UUID, Set<Item>> uniqueCrafts = LockoutServer.lockout.uniqueCrafts;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostUniqueCraftsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayer leader = server.getPlayerList().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = uniqueCrafts.getOrDefault(leaderUuid, Set.of()).size();
                tooltip.add(ChatFormatting.GOLD + "Leader: " + ChatFormatting.RESET + leader.getName().getString() + " - " + leaderCount);
            }
        }
        
        // Show team members
        for (String playerName : team.getPlayerNames()) {
            ServerPlayer teamPlayer = server.getPlayerList().getPlayer(playerName);
            if (teamPlayer != null) {
                UUID uuid = teamPlayer.getUUID();
                if (!uuid.equals(leaderUuid)) {
                    int count = uniqueCrafts.getOrDefault(uuid, Set.of()).size();
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
        Map<UUID, Set<Item>> uniqueCrafts = LockoutServer.lockout.uniqueCrafts;
        MinecraftServer server = LockoutServer.server;
        
        tooltip.add(" ");
        
        // Show leader
        UUID leaderUuid = LockoutServer.lockout.mostUniqueCraftsPlayer;
        if (leaderUuid != null && server != null) {
            ServerPlayer leader = server.getPlayerList().getPlayer(leaderUuid);
            if (leader != null) {
                int leaderCount = uniqueCrafts.getOrDefault(leaderUuid, Set.of()).size();
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
                        int count = uniqueCrafts.getOrDefault(uuid, Set.of()).size();
                        tooltip.add(team.getColor() + playerName + ChatFormatting.RESET + " - " + count);
                    }
                }
            }
        }
        
        tooltip.add(" ");
        return tooltip;
    }

}
