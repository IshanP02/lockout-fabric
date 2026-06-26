package me.marin.lockout.lockout.goals.misc;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;

public class PlacePaintingGoal extends IncrementItemStatGoal{

    private static final ItemStack ITEM = Items.PAINTING.getDefaultInstance();

    public PlacePaintingGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Stat<Item>> STATS = List.of(Stats.ITEM_USED.get(Items.PAINTING));
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
