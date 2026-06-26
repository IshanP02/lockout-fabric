package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

import java.util.List;

public class UseJukeboxGoal extends IncrementStatGoal {

    public UseJukeboxGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> STATS = List.of(Stats.PLAY_RECORD);
    @Override
    public List<Identifier> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Use Jukebox to play a Music Disc";
    }

    private static final ItemStack ITEM_STACK = Items.JUKEBOX.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
