package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class Obtain5UniquePressurePlatesGoal extends ObtainSomeOfTheItemsGoal {

    private static final ItemStack ITEM_STACK = Items.OAK_SAPLING.getDefaultInstance();
    static {
        ITEM_STACK.setCount(5);
    }
    private static final List<Item> ITEMS = List.of(
            Items.OAK_PRESSURE_PLATE,
            Items.ACACIA_PRESSURE_PLATE,
            Items.SPRUCE_PRESSURE_PLATE,
            Items.DARK_OAK_PRESSURE_PLATE,
            Items.CHERRY_PRESSURE_PLATE,
            Items.BIRCH_PRESSURE_PLATE,
            Items.MANGROVE_PRESSURE_PLATE,
            Items.PALE_OAK_PRESSURE_PLATE,
            Items.JUNGLE_PRESSURE_PLATE,
            Items.BAMBOO_PRESSURE_PLATE,
            Items.CRIMSON_PRESSURE_PLATE,
            Items.WARPED_PRESSURE_PLATE,
            Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Items.STONE_PRESSURE_PLATE,
            Items.POLISHED_BLACKSTONE_PRESSURE_PLATE
    );

    public Obtain5UniquePressurePlatesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 5;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 5 Unique Pressure Plates";
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

}