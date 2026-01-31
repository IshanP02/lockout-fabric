package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain4UniqueSeedsGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.TORCHFLOWER_SEEDS);
    private static final ItemStack ITEM_STACK = Items.WHEAT_SEEDS.getDefaultStack();

    public Obtain4UniqueSeedsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 4 Unique Seeds";
    }

    @Override
    public int getAmount() {
        return 4;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "4");
        return true;
    }
}
