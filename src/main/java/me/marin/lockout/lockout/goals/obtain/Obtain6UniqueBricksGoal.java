package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.Utility;
import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain6UniqueBricksGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(
            Items.BRICKS,
            Items.STONE_BRICKS,
            Items.MOSSY_STONE_BRICKS,
            Items.CRACKED_STONE_BRICKS,
            Items.CHISELED_STONE_BRICKS,
            Items.DEEPSLATE_BRICKS,
            Items.CRACKED_DEEPSLATE_BRICKS,
            Items.TUFF_BRICKS,
            Items.CHISELED_TUFF_BRICKS,
            Items.NETHER_BRICKS,
            Items.CRACKED_NETHER_BRICKS,
            Items.CHISELED_NETHER_BRICKS,
            Items.RED_NETHER_BRICKS,
            Items.END_STONE_BRICKS,
            Items.PRISMARINE_BRICKS,
            Items.QUARTZ_BRICKS,
            Items.MUD_BRICKS,
            Items.POLISHED_BLACKSTONE_BRICKS,
            Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Items.RESIN_BRICKS,
            Items.CHISELED_RESIN_BRICKS
    );

    public Obtain6UniqueBricksGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 6;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 6 Unique Brick Blocks";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        Utility.drawStackCount(context, x, y, String.valueOf(getAmount()));
        return true;
    }

}
