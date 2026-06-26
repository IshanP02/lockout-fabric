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

public class Obtain64ColoredWoolGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private final ItemStack ITEM_STACK;
    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public Obtain64ColoredWoolGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain 64 " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Wool";
        ITEMS = List.of(getWoolColor(data));
        ITEM_STACK = getWoolColor(data).getDefaultInstance();
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

    public static Item getWoolColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.WOOL.white();
            case "orange" -> Items.WOOL.orange();
            case "magenta" -> Items.WOOL.magenta();
            case "light_blue" -> Items.WOOL.lightBlue();
            case "yellow" -> Items.WOOL.yellow();
            case "lime" -> Items.WOOL.lime();
            case "pink" -> Items.WOOL.pink();
            case "gray" -> Items.WOOL.gray();
            case "light_gray" -> Items.WOOL.lightGray();
            case "cyan" -> Items.WOOL.cyan();
            case "purple" -> Items.WOOL.purple();
            case "blue" -> Items.WOOL.blue();
            case "brown" -> Items.WOOL.brown();
            case "green" -> Items.WOOL.green();
            case "red" -> Items.WOOL.red();
            case "black" -> Items.WOOL.black();
        };
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

}

