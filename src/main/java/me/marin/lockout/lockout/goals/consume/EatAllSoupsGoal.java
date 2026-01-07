package me.marin.lockout.lockout.goals.consume;

import me.marin.lockout.lockout.interfaces.ConsumeSomeOfTheFoodsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class EatAllSoupsGoal extends ConsumeSomeOfTheFoodsGoal {

    private static final ItemStack ITEM_STACK = Items.MUSHROOM_STEW.getDefaultStack();
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
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        // Use the cycling texture from parent, then add the overlay
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, ITEM_STACK, x, y, "4");
        return true;
    }
}
