package me.marin.lockout.lockout.goals.misc;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;

public class PlacePaintingGoal extends IncrementItemStatGoal{

    private static final ItemStack ITEM = Items.PAINTING.getDefaultStack();

    public PlacePaintingGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Stat<Item>> STATS = List.of(Stats.USED.getOrCreateStat(Items.PAINTING));
    @Override
    public List<Stat<Item>> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Place a Painting";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }
}
