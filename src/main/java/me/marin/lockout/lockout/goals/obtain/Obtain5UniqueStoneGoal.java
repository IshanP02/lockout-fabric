package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.Utility;
import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain5UniqueStoneGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(
            Items.STONE,
            Items.COBBLESTONE,
            Items.DIORITE,
            Items.ANDESITE,
            Items.GRANITE,
            Items.CALCITE,
            Items.TUFF,
            Items.DRIPSTONE_BLOCK,
            Items.DEEPSLATE,
            Items.COBBLED_DEEPSLATE
    );

    public Obtain5UniqueStoneGoal(String id, String data) {
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
        return "Obtain 5 Unique Stone Types";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        Utility.drawStackCount(context, x, y, String.valueOf(getAmount()));
        return true;
    }

}
