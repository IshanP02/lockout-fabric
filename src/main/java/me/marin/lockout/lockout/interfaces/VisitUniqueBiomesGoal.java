package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public abstract class VisitUniqueBiomesGoal extends Goal implements RequiresAmount, HasTooltipInfo, CustomTextureRenderer, Trackable<LockoutTeam, Set<Identifier>> {

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/unique_biomes.png");
    private final ItemStack DISPLAY_ITEM_STACK = Items.GRASS_BLOCK.getDefaultStack();

    public VisitUniqueBiomesGoal(String id, String data) {
        super(id, data);
        DISPLAY_ITEM_STACK.setCount(getAmount());
    }

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, DISPLAY_ITEM_STACK, x, y);
        return true;
    }

    @Override
    public Map<LockoutTeam, Set<Identifier>> getTrackerMap() {
        return LockoutServer.lockout.biomesVisited;
    }

    @Override
public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
    List<String> tooltip = new ArrayList<>();
    var biomes = getTrackerMap().getOrDefault(team, new LinkedHashSet<Identifier>());

    tooltip.add(" ");
    tooltip.add("Biomes: " + biomes.size() + "/" + getAmount());

    // Map identifiers -> "namespace:path (Localized Name)" (fallback to readable name)
    List<String> names = biomes.stream()
        .map(id -> {
            String readable;
            try {
                String key = "biome." + id.getNamespace() + "." + id.getPath();
                readable = Text.translatable(key).getString();
            } catch (Exception e) {
                // fallback readable name from id path: "snowy_taiga" -> "Snowy Taiga"
                String path = id.getPath();
                String[] parts = path.split("_");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].isEmpty()) continue;
                    parts[i] = parts[i].substring(0,1).toUpperCase() + parts[i].substring(1).toLowerCase();
                }
                readable = String.join(" ", parts);
            }
            return readable;
        })
        .collect(Collectors.toList());

        // Add all biome entries to the tooltip (format: namespace:path (Localized Name))
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(names));
        tooltip.add(" ");
        return tooltip;
}

@Override
public List<String> getSpectatorTooltip() {
    List<String> tooltip = new ArrayList<>();
    tooltip.add(" ");
    for (LockoutTeam t : LockoutServer.lockout.getTeams()) {
        var set = getTrackerMap().getOrDefault(t, new LinkedHashSet<Identifier>());
        tooltip.add(t.getColor() + t.getDisplayName() + Formatting.RESET + ": " + set.size() + "/" + getAmount());
    }
    tooltip.add(" ");
    return tooltip;
}

}
