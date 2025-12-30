package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class GetHotTouristDestinationsAdvancementGoal extends AdvancementGoal implements CycleItemTexturesProvider, HasTooltipInfo {

    private static final ItemStack ITEM_STACK = Items.CRIMSON_NYLIUM.getDefaultStack();
    private static final List<Item> ITEMS_TO_DISPLAY = List.of(Items.CRIMSON_NYLIUM, Items.WARPED_NYLIUM, Items.SOUL_SOIL, Items.NETHERRACK, Items.BASALT);
    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "nether/explore_nether"));

    public GetHotTouristDestinationsAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Visit All Nether Biomes";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }

    @Override
    public List<Item> getItemsToDisplay() {
        return ITEMS_TO_DISPLAY;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, PlayerEntity player) {
        List<String> tooltip = new ArrayList<>();
        var biomes = LockoutServer.lockout.biomesVisited.getOrDefault(team, new LinkedHashSet<>());
        
        // Filter to only show nether biomes
        var netherBiomes = biomes.stream()
            .filter(id -> id.getPath().contains("nether") || 
                         id.getPath().contains("basalt") || 
                         id.getPath().contains("warped") || 
                         id.getPath().contains("crimson") || 
                         id.getPath().contains("soul_sand"))
            .collect(Collectors.toSet());

        tooltip.add(" ");
        tooltip.add("Nether Biomes: " + netherBiomes.size() + "/5");
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(
            netherBiomes.stream()
                .map(id -> Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString())
                .collect(Collectors.toList())
        ));
        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(" ");
        for (LockoutTeam t : LockoutServer.lockout.getTeams()) {
            var biomes = LockoutServer.lockout.biomesVisited.getOrDefault(t, new LinkedHashSet<>());
            
            // Filter to only show nether biomes
            var netherBiomes = biomes.stream()
                .filter(id -> id.getPath().contains("nether") || 
                             id.getPath().contains("basalt") || 
                             id.getPath().contains("warped") || 
                             id.getPath().contains("crimson") || 
                             id.getPath().contains("soul_sand"))
                .collect(Collectors.toSet());
            
            tooltip.add(t.getColor() + t.getDisplayName() + Formatting.RESET + ": " + netherBiomes.size() + "/5");
        }
        tooltip.add(" ");
        return tooltip;
    }
}
