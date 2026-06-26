package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class UseGoldenDandelionOnBabyMobGoal extends Goal {

    public UseGoldenDandelionOnBabyMobGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Golden Dandelion on Baby Mob";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return Items.GOLDEN_DANDELION.getDefaultInstance();
    }
}
