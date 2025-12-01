package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain3UniqueBannerPatternsGoal extends ObtainSomeOfTheItemsGoal {

    private static final ItemStack ITEM_STACK = Items.BORDURE_INDENTED_BANNER_PATTERN.getDefaultStack();
    static {
        ITEM_STACK.setCount(3);
    }
    private static final List<Item> ITEMS = List.of(
            Items.BORDURE_INDENTED_BANNER_PATTERN, 
            Items.CREEPER_BANNER_PATTERN,
            Items.FLOWER_BANNER_PATTERN,
            Items.GLOBE_BANNER_PATTERN,
            Items.SKULL_BANNER_PATTERN,
            Items.MOJANG_BANNER_PATTERN,
            Items.FIELD_MASONED_BANNER_PATTERN,
            Items.FLOW_BANNER_PATTERN,
            Items.GUSTER_BANNER_PATTERN,
            Items.PIGLIN_BANNER_PATTERN
    );

    public Obtain3UniqueBannerPatternsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 3;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 3 Unique Banner Patterns";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "3");
        return true;
    }
}
