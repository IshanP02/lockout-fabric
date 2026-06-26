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

public class Obtain64ColoredTerracottaGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private final ItemStack ITEM_STACK;
    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public Obtain64ColoredTerracottaGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain 64 " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Terracotta";
        ITEMS = List.of(getTerracottaColor(data));
        ITEM_STACK = getTerracottaColor(data).getDefaultInstance();
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
            case "white" -> Items.DYED_TERRACOTTA.white();
            case "orange" -> Items.DYED_TERRACOTTA.orange();
            case "magenta" -> Items.DYED_TERRACOTTA.magenta();
            case "light_blue" -> Items.DYED_TERRACOTTA.lightBlue();
            case "yellow" -> Items.DYED_TERRACOTTA.yellow();
            case "lime" -> Items.DYED_TERRACOTTA.lime();
            case "pink" -> Items.DYED_TERRACOTTA.pink();
            case "gray" -> Items.DYED_TERRACOTTA.gray();
            case "light_gray" -> Items.DYED_TERRACOTTA.lightGray();
            case "cyan" -> Items.DYED_TERRACOTTA.cyan();
            case "purple" -> Items.DYED_TERRACOTTA.purple();
            case "blue" -> Items.DYED_TERRACOTTA.blue();
            case "brown" -> Items.DYED_TERRACOTTA.brown();
            case "green" -> Items.DYED_TERRACOTTA.green();
            case "red" -> Items.DYED_TERRACOTTA.red();
            case "black" -> Items.DYED_TERRACOTTA.black();
        };
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

}

