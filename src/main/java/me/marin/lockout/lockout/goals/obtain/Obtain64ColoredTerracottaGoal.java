package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import me.marin.lockout.lockout.interfaces.RequiresAmount;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.List;

public class Obtain64ColoredTerracottaGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private final ItemStack ITEM_STACK;
    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public Obtain64ColoredTerracottaGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain 64 " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Terracotta";
        ITEMS = List.of(getTerracottaColor(data));
        ITEM_STACK = getTerracottaColor(data).getDefaultStack();
        ITEM_STACK.setCount(64);
    }

    @Override
    public int getAmount() {
        return 64;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return GOAL_NAME;
    }

    public static Item getTerracottaColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.WHITE_TERRACOTTA;
            case "orange" -> Items.ORANGE_TERRACOTTA;
            case "magenta" -> Items.MAGENTA_TERRACOTTA;
            case "light_blue" -> Items.LIGHT_BLUE_TERRACOTTA;
            case "yellow" -> Items.YELLOW_TERRACOTTA;
            case "lime" -> Items.LIME_TERRACOTTA;
            case "pink" -> Items.PINK_TERRACOTTA;
            case "gray" -> Items.GRAY_TERRACOTTA;
            case "light_gray" -> Items.LIGHT_GRAY_TERRACOTTA;
            case "cyan" -> Items.CYAN_TERRACOTTA;
            case "purple" -> Items.PURPLE_TERRACOTTA;
            case "blue" -> Items.BLUE_TERRACOTTA;
            case "brown" -> Items.BROWN_TERRACOTTA;
            case "green" -> Items.GREEN_TERRACOTTA;
            case "red" -> Items.RED_TERRACOTTA;
            case "black" -> Items.BLACK_TERRACOTTA;
        };
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, ITEM_STACK, x, y);
        return true;
    }

}

