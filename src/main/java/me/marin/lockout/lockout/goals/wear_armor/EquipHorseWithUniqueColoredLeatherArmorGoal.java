package me.marin.lockout.lockout.goals.wear_armor;

import me.marin.lockout.lockout.goals.util.GoalDataConstants;

import me.marin.lockout.lockout.Goal;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

public class EquipHorseWithUniqueColoredLeatherArmorGoal extends Goal {

    private final Item ITEM;
    private final ItemStack DISPLAY_ITEM_STACK;
    private final int COLOR;
    private final String GOAL_NAME;

    public EquipHorseWithUniqueColoredLeatherArmorGoal(String id, String data) {
        super(id, data);

        String[] parts = data.split(GoalDataConstants.DATA_SEPARATOR);
        ITEM = GoalDataConstants.getLeatherHorseArmor(parts[0]);
        DyeColor DYE_COLOR = GoalDataConstants.getDyeColor(parts[1]);
        COLOR = GoalDataConstants.getDyeColorValue(DYE_COLOR);

        DISPLAY_ITEM_STACK = ITEM.getDefaultStack();
        DISPLAY_ITEM_STACK.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(COLOR));

        GOAL_NAME = "Equip Horse with " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " " + GoalDataConstants.getHorseArmorPieceFormatted(parts[0]);
    }

    @Override
    public String getGoalName() {
        return GOAL_NAME;
    }

    public int getColorValue() {
        return this.COLOR;
    }

    public Item getItemType() {
        return this.ITEM;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

}
