package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Boat2KmGoal extends Goal implements CustomTextureRenderer, HasTooltipInfo {

    private static final int TWO_KILOMETER = 100 * 2000; // in cm

    private static final Item ITEM = Items.OAK_BOAT;
    private static final ItemStack ITEM_STACK = Items.OAK_BOAT.getDefaultInstance();
    public Boat2KmGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Boat 2km";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.item(ITEM_STACK, x, y);
        context.itemDecorations(Minecraft.getInstance().font,  ITEM_STACK, x, y, "2km");
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        int distance = Math.min(TWO_KILOMETER, LockoutServer.lockout.distanceByBoat.getOrDefault(player.getUUID(), 0));
        LockoutTeamServer serverTeam = (LockoutTeamServer) team;

        tooltip.add(" ");
        tooltip.add("Distance Boated: " + String.format("%.2fm", distance / 100.0));
        if (serverTeam.getPlayers().size() > 1) {
            tooltip.add(" ");
            for (UUID uuid : serverTeam.getPlayers()) {
                if (!Objects.equals(uuid, player.getUUID())) {
                    int pdist = Math.min(TWO_KILOMETER, LockoutServer.lockout.distanceByBoat.getOrDefault(uuid, 0));
                    tooltip.add(serverTeam.getPlayerName(uuid) + ": " + String.format("%.2fm", pdist / 100.0));
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
                max = Math.max(max, LockoutServer.lockout.distanceByBoat.getOrDefault(uuid, 0));
            }
            tooltip.add(t.getColor() + t.getDisplayName() + ChatFormatting.RESET + ": " + String.format("%.2fm", Math.min(TWO_KILOMETER, max) / 100.0));
        }
        tooltip.add(" ");

        return tooltip;
    }

}
