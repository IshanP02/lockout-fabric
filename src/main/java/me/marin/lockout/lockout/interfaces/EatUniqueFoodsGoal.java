package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class EatUniqueFoodsGoal extends Goal implements RequiresAmount, Trackable<LockoutTeam, LinkedHashSet<Item>>, CustomTextureRenderer, HasTooltipInfo {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/eat_unique.png");
    private final ItemStack DISPLAY_ITEM_STACK = Items.APPLE.getDefaultInstance();

    public EatUniqueFoodsGoal(String id, String data) {
        super(id, data);
        DISPLAY_ITEM_STACK.setCount(getAmount());
    }

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.itemDecorations(Minecraft.getInstance().font, DISPLAY_ITEM_STACK, x, y);
        return true;
    }

    @Override
    public Map<LockoutTeam, LinkedHashSet<Item>> getTrackerMap() {
        return LockoutServer.lockout.foodTypesEaten;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        var foods = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Unique Food types: " + foods.size() + "/" + getAmount());
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(foods.stream().map(item -> item.getName(item.getDefaultInstance()).getString()).toList()));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            var foods = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());
            tooltip.add(team.getColor() + team.getDisplayName() + ChatFormatting.RESET + ": " + foods.size() + "/" + getAmount());
        }
        tooltip.add(" ");

        return tooltip;
    }

}
