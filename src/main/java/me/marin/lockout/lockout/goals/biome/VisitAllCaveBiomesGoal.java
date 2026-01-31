package me.marin.lockout.lockout.goals.biome;

import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.interfaces.VisitAllSpecificBiomesGoal;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class VisitAllCaveBiomesGoal extends VisitAllSpecificBiomesGoal implements CycleItemTexturesProvider {

    private static final ItemStack ITEM_STACK = Items.SCULK.getDefaultStack();
    private static final List<Item> ITEMS_TO_DISPLAY = List.of(Items.MOSS_BLOCK, Items.DRIPSTONE_BLOCK, Items.SCULK);
    private static final List<Identifier> BIOMES = List.of(
            Identifier.of("minecraft", "lush_caves"),
            Identifier.of("minecraft", "dripstone_caves"),
            Identifier.of("minecraft", "deep_dark")
    );

    static {
        ITEM_STACK.setCount(BIOMES.size());
    }

    public VisitAllCaveBiomesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Visit all Cave Biomes";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<Identifier> getBiomes() {
        return BIOMES;
    }

    @Override
    public Map<LockoutTeam, LinkedHashSet<Identifier>> getTrackerMap() {
        return LockoutServer.lockout.visitedSpecificBiomes;
    }

    @Override
    public List<Item> getItemsToDisplay() {
        return ITEMS_TO_DISPLAY;
    }
}
