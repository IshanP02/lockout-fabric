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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public abstract class VisitUniqueBiomesGoal extends Goal implements RequiresAmount, HasTooltipInfo, CustomTextureRenderer, Trackable<LockoutTeam, Set<Identifier>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/unique_biomes.png");
    private final ItemStack DISPLAY_ITEM_STACK = Items.GRASS_BLOCK.getDefaultInstance();

    public VisitUniqueBiomesGoal(String id, String data) {
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
    public Map<LockoutTeam, Set<Identifier>> getTrackerMap() {
        return LockoutServer.lockout.biomesVisited;
    }

    @Override
public List<String> getTooltip(LockoutTeam team, Player player) {
    List<String> tooltip = new ArrayList<>();
    var biomes = getTrackerMap().getOrDefault(team, new LinkedHashSet<Identifier>());

    tooltip.add(" ");
    tooltip.add("Biomes: " + biomes.size() + "/" + getAmount());
    tooltip.addAll(HasTooltipInfo.commaSeparatedList(biomes.stream().map(id -> Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString()).collect(Collectors.toList())));
    return tooltip;
}

@Override
public List<String> getSpectatorTooltip() {
    List<String> tooltip = new ArrayList<>();
    tooltip.add(" ");
    for (LockoutTeam t : LockoutServer.lockout.getTeams()) {
        var set = getTrackerMap().getOrDefault(t, new LinkedHashSet<Identifier>());
        tooltip.add(t.getColor() + t.getDisplayName() + ChatFormatting.RESET + ": " + set.size() + "/" + getAmount());
    }
    tooltip.add(" ");
    return tooltip;
}

}
