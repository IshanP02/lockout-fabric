package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.ObtainAllItemsGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public class ObtainColoredGlazedTerracottaGoal extends ObtainAllItemsGoal {

    private final List<Item> ITEMS;

    private final String GOAL_NAME;

    public ObtainColoredGlazedTerracottaGoal(String id, String data) {
        super(id, data);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(data);

        GOAL_NAME = "Obtain " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Glazed Terracotta";
        ITEMS = List.of(getGlazedTerracottaColor(data));
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return GOAL_NAME;
    }

    public static Item getGlazedTerracottaColor(String colorString) {
        return switch (colorString) {
            default -> null;
            case "white" -> Items.GLAZED_TERRACOTTA.white();
            case "orange" -> Items.GLAZED_TERRACOTTA.orange();
            case "magenta" -> Items.GLAZED_TERRACOTTA.magenta();
            case "light_blue" -> Items.GLAZED_TERRACOTTA.lightBlue();
            case "yellow" -> Items.GLAZED_TERRACOTTA.yellow();
            case "lime" -> Items.GLAZED_TERRACOTTA.lime();
            case "pink" -> Items.GLAZED_TERRACOTTA.pink();
            case "gray" -> Items.GLAZED_TERRACOTTA.gray();
            case "light_gray" -> Items.GLAZED_TERRACOTTA.lightGray();
            case "cyan" -> Items.GLAZED_TERRACOTTA.cyan();
            case "purple" -> Items.GLAZED_TERRACOTTA.purple();
            case "blue" -> Items.GLAZED_TERRACOTTA.blue();
            case "brown" -> Items.GLAZED_TERRACOTTA.brown();
            case "green" -> Items.GLAZED_TERRACOTTA.green();
            case "red" -> Items.GLAZED_TERRACOTTA.red();
            case "black" -> Items.GLAZED_TERRACOTTA.black();
        };
    }

}
