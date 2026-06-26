package me.marin.lockout.lockout.goals.kill.unique;

import me.marin.lockout.lockout.interfaces.KillUniqueHostileMobsGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Kill15UniqueHostileMobsGoal extends KillUniqueHostileMobsGoal {

    private final ItemStack ITEM = Items.IRON_SWORD.getDefaultInstance();

    public Kill15UniqueHostileMobsGoal(String id, String data) {
        super(id, data);
        ITEM.setCount(getAmount());
    }

    @Override
    public String getGoalName() {
        return "Kill 15 Unique Hostile Mobs";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM;
    }

    @Override
    public int getAmount() {
        return 15;
    }

}
