package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import me.marin.lockout.lockout.interfaces.RequiresAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

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
        ITEM_STACK = getGlassColor(data).getDefaultInstance();
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
            case "white" -> Items.STAINED_GLASS.white();
            case "orange" -> Items.STAINED_GLASS.orange();
            case "magenta" -> Items.STAINED_GLASS.magenta();
            case "light_blue" -> Items.STAINED_GLASS.lightBlue();
            case "yellow" -> Items.STAINED_GLASS.yellow();
            case "lime" -> Items.STAINED_GLASS.lime();
            case "pink" -> Items.STAINED_GLASS.pink();
            case "gray" -> Items.STAINED_GLASS.gray();
            case "light_gray" -> Items.STAINED_GLASS.lightGray();
            case "cyan" -> Items.STAINED_GLASS.cyan();
            case "purple" -> Items.STAINED_GLASS.purple();
            case "blue" -> Items.STAINED_GLASS.blue();
            case "brown" -> Items.STAINED_GLASS.brown();
            case "green" -> Items.STAINED_GLASS.green();
            case "red" -> Items.STAINED_GLASS.red();
            case "black" -> Items.STAINED_GLASS.black();
        };
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

}

