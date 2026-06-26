package me.marin.lockout.lockout.goals.workstation;

import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

import java.util.List;

public class UseEnchantingTableGoal extends IncrementStatGoal {

    public UseEnchantingTableGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> STATS = List.of(Stats.ENCHANT_ITEM);
    @Override
    public List<Identifier> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Use Enchanting Table";
    }

    private static final ItemStack ITEM_STACK = Items.ENCHANTING_TABLE.getDefaultInstance();
    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

}
