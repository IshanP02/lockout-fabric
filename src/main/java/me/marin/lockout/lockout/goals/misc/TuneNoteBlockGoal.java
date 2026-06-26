package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

import java.util.List;

public class TuneNoteBlockGoal extends IncrementStatGoal {

    private static final Item ITEMS = Items.NOTE_BLOCK;
    public TuneNoteBlockGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> STATS = List.of(Stats.TUNE_NOTEBLOCK);
    @Override
    public List<Identifier> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Tune a Note Block";
    }

    
    @Override
    public ItemStack getTextureItemStack() {
        return ITEMS.getDefaultInstance();
    }

}