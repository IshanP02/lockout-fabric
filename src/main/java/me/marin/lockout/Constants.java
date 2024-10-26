package me.marin.lockout;

import net.minecraft.util.Identifier;

public class Constants {

    public static final String NAMESPACE = "lockout";

    public static final Identifier LOCKOUT_GOALS_TEAMS_PACKET = Identifier.of(NAMESPACE, "lockout_goals_teams");
    public static final Identifier START_LOCKOUT_PACKET = Identifier.of(NAMESPACE, "start_lockout");
    public static final Identifier UPDATE_TOOLTIP = Identifier.of(NAMESPACE, "update_tooltip");
    public static final Identifier COMPLETE_TASK_PACKET = Identifier.of(NAMESPACE, "complete_task");
    public static final Identifier END_LOCKOUT_PACKET = Identifier.of(NAMESPACE, "end_lockout");
    public static final Identifier UPDATE_TIMER_PACKET = Identifier.of(NAMESPACE, "update_timer");

    public static final Identifier CUSTOM_BOARD_PACKET = Identifier.of(NAMESPACE, "set_custom_board");

    public static final Identifier BOARD_SCREEN_ID = Identifier.of(NAMESPACE, "board");

    public static final Identifier BOARD_FILE_ARGUMENT_TYPE = Identifier.of(NAMESPACE, "board_file");

    public static final Identifier GUI_IDENTIFIER = Identifier.of(NAMESPACE, "textures/guis/gui.png");

    public static final int GUI_WIDTH = 94;
    public static final int GUI_HEIGHT = 105;
    public static final int GUI_FIRST_ITEM_OFFSET = 3; // both x and y
    public static final int GUI_ITEM_SLOT_SIZE = 18; // both x and y

    public static final Identifier GUI_CENTER_IDENTIFIER = Identifier.of(NAMESPACE, "textures/guis/gui_center.png");
    public static final int GUI_CENTER_WIDTH = 104;
    public static final int GUI_CENTER_HEIGHT = 105;
    public static final int GUI_CENTER_FIRST_ITEM_OFFSET_X = 8;
    public static final int GUI_CENTER_FIRST_ITEM_OFFSET_Y = 9;
    public static final int GUI_CENTER_ITEM_SLOT_SIZE = 18; // both x and y

}
