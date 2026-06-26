package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.interfaces.Trackable;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DamagedByUniqueSourcesGoal extends Goal implements Trackable<LockoutTeam, LinkedHashSet<ResourceKey<DamageType>>>, TextureProvider, HasTooltipInfo {

    private final static ItemStack DISPLAY_ITEM_STACK = Items.DYE.red().getDefaultInstance();
    static {
        DISPLAY_ITEM_STACK.setCount(64);
    }
    public DamagedByUniqueSourcesGoal(String id, String data) {
        super(id, data);
    }

    public abstract int getAmount();

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/take_unique_damage.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        LinkedHashSet<net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>> set = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Unique Sources: " + Math.min(getAmount(), set.size()) + "/" + getAmount());
        // list formatted damage type names
        List<String> names = set.stream()
            .map(k -> org.apache.commons.lang3.text.WordUtils.capitalize(k.identifier().getPath().replace("_", " "), ' '))
            .collect(Collectors.toList());
        if (!names.isEmpty()) {
            tooltip.addAll(HasTooltipInfo.commaSeparatedList(names));
        }
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            int damage = LockoutServer.lockout.damageByUniqueSources.getOrDefault(team, 0);
            tooltip.add(team.getColor() + team.getDisplayName() + ChatFormatting.RESET + ": " + Math.min(getAmount(), damage) + "/" + getAmount());
        }
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public Map<LockoutTeam, LinkedHashSet<ResourceKey<DamageType>>> getTrackerMap() {
        return LockoutServer.lockout.damageTypesTaken;
    }

}
