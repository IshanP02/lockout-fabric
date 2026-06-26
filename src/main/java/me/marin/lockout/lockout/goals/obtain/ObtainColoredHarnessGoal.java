package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

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
            case "white" -> Items.HARNESS.white();
            case "orange" -> Items.HARNESS.orange();
            case "magenta" -> Items.HARNESS.magenta();
            case "light_blue" -> Items.HARNESS.lightBlue();
            case "yellow" -> Items.HARNESS.yellow();
            case "lime" -> Items.HARNESS.lime();
            case "pink" -> Items.HARNESS.pink();
            case "gray" -> Items.HARNESS.gray();
            case "light_gray" -> Items.HARNESS.lightGray();
            case "cyan" -> Items.HARNESS.cyan();
            case "purple" -> Items.HARNESS.purple();
            case "blue" -> Items.HARNESS.blue();
            case "brown" -> Items.HARNESS.brown();
            case "green" -> Items.HARNESS.green();
            case "red" -> Items.HARNESS.red();
            case "black" -> Items.HARNESS.black();
        };
    }

}

