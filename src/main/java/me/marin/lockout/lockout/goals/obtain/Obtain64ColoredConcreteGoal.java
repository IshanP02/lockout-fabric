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

public class Obtain64ColoredConcreteGoal extends ObtainAllItemsGoal implements RequiresAmount {

    private final ItemStack ITEM_STACK;
    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public Obtain64ColoredConcreteGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain 64 " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Concrete";
        ITEMS = List.of(getConcreteColor(data));
        ITEM_STACK = getConcreteColor(data).getDefaultInstance();
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

    public static Item getConcreteColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.CONCRETE.white();
            case "orange" -> Items.CONCRETE.orange();
            case "magenta" -> Items.CONCRETE.magenta();
            case "light_blue" -> Items.CONCRETE.lightBlue();
            case "yellow" -> Items.CONCRETE.yellow();
            case "lime" -> Items.CONCRETE.lime();
            case "pink" -> Items.CONCRETE.pink();
            case "gray" -> Items.CONCRETE.gray();
            case "light_gray" -> Items.CONCRETE.lightGray();
            case "cyan" -> Items.CONCRETE.cyan();
            case "purple" -> Items.CONCRETE.purple();
            case "blue" -> Items.CONCRETE.blue();
            case "brown" -> Items.CONCRETE.brown();
            case "green" -> Items.CONCRETE.green();
            case "red" -> Items.CONCRETE.red();
            case "black" -> Items.CONCRETE.black();
        };
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

}

