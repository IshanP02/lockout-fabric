package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.Utility;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import me.marin.lockout.lockout.interfaces.RequiresAmount;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class Obtain64NameTagsGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private static final ItemStack ITEM_STACK = Items.NAME_TAG.getDefaultInstance();
    static {
        ITEM_STACK.setCount(64);
    }
    private static final List<Item> ITEMS = List.of(ITEM_STACK.getItem());

    public Obtain64NameTagsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 64 Name Tags";
    }

    @Override
    public int getAmount() {
        return 64;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        Utility.drawStackCount(context, x, y, String.valueOf(getAmount()));
        return true;
    }

}
