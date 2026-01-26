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

public class Obtain64ColoredGlassGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private final ItemStack ITEM_STACK;
    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public Obtain64ColoredGlassGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain 64 " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Glass";
        ITEMS = List.of(getGlassColor(data));
        ITEM_STACK = getGlassColor(data).getDefaultStack();
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

    public static Item getGlassColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.WHITE_STAINED_GLASS;
            case "orange" -> Items.ORANGE_STAINED_GLASS;
            case "magenta" -> Items.MAGENTA_STAINED_GLASS;
            case "light_blue" -> Items.LIGHT_BLUE_STAINED_GLASS;
            case "yellow" -> Items.YELLOW_STAINED_GLASS;
            case "lime" -> Items.LIME_STAINED_GLASS;
            case "pink" -> Items.PINK_STAINED_GLASS;
            case "gray" -> Items.GRAY_STAINED_GLASS;
            case "light_gray" -> Items.LIGHT_GRAY_STAINED_GLASS;
            case "cyan" -> Items.CYAN_STAINED_GLASS;
            case "purple" -> Items.PURPLE_STAINED_GLASS;
            case "blue" -> Items.BLUE_STAINED_GLASS;
            case "brown" -> Items.BROWN_STAINED_GLASS;
            case "green" -> Items.GREEN_STAINED_GLASS;
            case "red" -> Items.RED_STAINED_GLASS;
            case "black" -> Items.BLACK_STAINED_GLASS;
        };
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, ITEM_STACK, x, y);
        return true;
    }

}

