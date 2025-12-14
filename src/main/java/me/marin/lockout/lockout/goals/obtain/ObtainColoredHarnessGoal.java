package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.List;

public class ObtainColoredHarnessGoal extends ObtainAllItemsGoal {

    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public ObtainColoredHarnessGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Harness";
        ITEMS = List.of(getHarnessColor(data));
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return GOAL_NAME;
    }

    public static Item getHarnessColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.WHITE_HARNESS;
            case "orange" -> Items.ORANGE_HARNESS;
            case "magenta" -> Items.MAGENTA_HARNESS;
            case "light_blue" -> Items.LIGHT_BLUE_HARNESS;
            case "yellow" -> Items.YELLOW_HARNESS;
            case "lime" -> Items.LIME_HARNESS;
            case "pink" -> Items.PINK_HARNESS;
            case "gray" -> Items.GRAY_HARNESS;
            case "light_gray" -> Items.LIGHT_GRAY_HARNESS;
            case "cyan" -> Items.CYAN_HARNESS;
            case "purple" -> Items.PURPLE_HARNESS;
            case "blue" -> Items.BLUE_HARNESS;
            case "brown" -> Items.BROWN_HARNESS;
            case "green" -> Items.GREEN_HARNESS;
            case "red" -> Items.RED_HARNESS;
            case "black" -> Items.BLACK_HARNESS;
        };
    }

}

