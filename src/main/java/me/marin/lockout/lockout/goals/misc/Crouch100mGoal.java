package me.marin.lockout.lockout.goals.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public class Crouch100mGoal extends Goal implements CustomTextureRenderer, HasTooltipInfo {

    private static final int ONE_HUNDRED_METERS = 100 * 100; // in cm

    private static final ItemStack ITEM_STACK = Items.SUGAR.getDefaultInstance();
    public Crouch100mGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Crouch 100m";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/sneak_100m.png");
    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.itemDecorations(Minecraft.getInstance().font,  ITEM_STACK, x, y, "1hm");
        return true;
    }
    
    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        int distance = Math.min(ONE_HUNDRED_METERS, LockoutServer.lockout.distanceCrouched.getOrDefault(player.getUUID(), 0));
        LockoutTeamServer serverTeam = ((LockoutTeamServer) team);

        tooltip.add(" ");
        tooltip.add("Distance Crouched: " + String.format("%.2fm", distance / 100.0));
        if (serverTeam.getPlayers().size() > 1) {
            tooltip.add(" ");
            for (UUID uuid : serverTeam.getPlayers()) {
                if (!Objects.equals(uuid, player.getUUID())) {
                    int dist = Math.min(ONE_HUNDRED_METERS, LockoutServer.lockout.distanceCrouched.getOrDefault(uuid, 0));
                    tooltip.add(serverTeam.getPlayerName(uuid) + ": " + String.format("%.2fm", dist / 100.0));
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
        for (LockoutTeam t : LockoutServer.lockout.getTeams()) {
            LockoutTeamServer serverTeam = (LockoutTeamServer) t;
            int max = 0;
            for (UUID uuid : serverTeam.getPlayers()) {
                max = Math.max(max, LockoutServer.lockout.distanceCrouched.getOrDefault(uuid, 0));
            }
            tooltip.add(t.getColor() + t.getDisplayName() + ChatFormatting.RESET + ": " + String.format("%.2fm", Math.min(ONE_HUNDRED_METERS, max) / 100.0));
        }
        tooltip.add(" ");

        return tooltip;
    }

}