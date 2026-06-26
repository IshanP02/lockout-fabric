package me.marin.lockout.lockout.goals.mine;

import me.marin.lockout.lockout.interfaces.MineBlockGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class MineTurtleEggGoal extends MineBlockGoal {

    private static final List<Item> ITEMS = List.of(Items.TURTLE_EGG);
    private static final Item ITEM = Items.TURTLE_EGG;

    public MineTurtleEggGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Mine Turtle Egg";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

}

