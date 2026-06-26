package me.marin.lockout.lockout.goals.consume;

import me.marin.lockout.lockout.interfaces.ConsumeSomeOfTheFoodsGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class EatAllSoupsGoal extends ConsumeSomeOfTheFoodsGoal {

    private static final ItemStack ITEM_STACK = Items.MUSHROOM_STEW.getDefaultInstance();
    static {
        ITEM_STACK.setCount(4);
    }
    private static final List<Item> ITEMS = List.of(
            Items.MUSHROOM_STEW,
            Items.RABBIT_STEW,
            Items.BEETROOT_SOUP,
            Items.SUSPICIOUS_STEW
    );

    public EatAllSoupsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 4;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Eat All Types of Soup";
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        // Use the cycling texture from parent, then add the overlay
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y, "4");
        return true;
    }
}
