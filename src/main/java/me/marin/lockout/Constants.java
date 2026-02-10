package me.marin.lockout;

import net.minecraft.util.Identifier;

public class Constants {

    public static final String NAMESPACE = "lockout";

    public static final int MIN_BOARD_SIZE = 1;
    public static final int MAX_BOARD_SIZE = 12;

    public static final Identifier LOCKOUT_GOALS_TEAMS_PACKET = Identifier.of(NAMESPACE, "lockout_goals_teams");
    public static final Identifier START_LOCKOUT_PACKET = Identifier.of(NAMESPACE, "start_lockout");
    public static final Identifier UPDATE_TOOLTIP = Identifier.of(NAMESPACE, "update_tooltip");
    public static final Identifier COMPLETE_TASK_PACKET = Identifier.of(NAMESPACE, "complete_task");
    public static final Identifier END_LOCKOUT_PACKET = Identifier.of(NAMESPACE, "end_lockout");
    public static final Identifier UPDATE_TIMER_PACKET = Identifier.of(NAMESPACE, "update_timer");
    public static final Identifier LOCKOUT_VERSION_PACKET = Identifier.of(NAMESPACE, "lockout_version");

    public static final Identifier CUSTOM_BOARD_PACKET = Identifier.of(NAMESPACE, "set_custom_board");
    public static final Identifier UPDATE_PICKS_BANS_PACKET = Identifier.of(NAMESPACE, "update_picks_bans");
    public static final Identifier BROADCAST_PICK_BAN_PACKET = Identifier.of(NAMESPACE, "broadcast_pick_ban");
    public static final Identifier SYNC_PICK_BAN_LIMIT_PACKET = Identifier.of(NAMESPACE, "sync_pick_ban_limit");
    public static final Identifier SET_BOARD_TYPE_PACKET = Identifier.of(NAMESPACE, "set_board_type");
    public static final Identifier UPLOAD_BOARD_TYPE_PACKET = Identifier.of(NAMESPACE, "upload_board_type");
    public static final Identifier ANNOUNCE_GOAL_FOCUS_PACKET = Identifier.of(NAMESPACE, "announce_goal_focus");
    public static final Identifier REQUEST_GOAL_DETAILS_PACKET = Identifier.of(NAMESPACE, "request_goal_details");
    public static final Identifier GOAL_DETAILS_PACKET = Identifier.of(NAMESPACE, "goal_details");
    
    public static final Identifier START_PICK_BAN_SESSION_PACKET = Identifier.of(NAMESPACE, "start_pick_ban_session");
    public static final Identifier UPDATE_PICK_BAN_SESSION_PACKET = Identifier.of(NAMESPACE, "update_pick_ban_session");
    public static final Identifier LOCK_PICK_BAN_SELECTIONS_PACKET = Identifier.of(NAMESPACE, "lock_pick_ban_selections");
    public static final Identifier END_PICK_BAN_SESSION_PACKET = Identifier.of(NAMESPACE, "end_pick_ban_session");

    public static final Identifier BOARD_SCREEN_ID = Identifier.of(NAMESPACE, "board");

    public static final Identifier BOARD_FILE_ARGUMENT_TYPE = Identifier.of(NAMESPACE, "board_file");
    public static final Identifier BOARD_POSITION_ARGUMENT_TYPE = Identifier.of(NAMESPACE, "board_position");

    public static final Identifier GUI_IDENTIFIER = Identifier.of(NAMESPACE, "gui");

    public static final int GUI_PADDING = 2; // both x and y
    public static final int GUI_PADDING_BOTTOM = 13; // both x and y
    public static final int GUI_SLOT_SIZE = 18; // both x and y

    public static final Identifier GUI_CENTER_IDENTIFIER = Identifier.of(NAMESPACE, "gui_center");
    public static final int GUI_CENTER_PADDING = 7;
    public static final int GUI_CENTER_SLOT_SIZE = 18; // both x and y

    public static final int GUI_CENTER_HOVERED_COLOR = -2130706433;

    public static final String BOARD_POSITION_LEFT = "left";
    public static final String BOARD_POSITION_RIGHT = "right";

    public static final String PLACEHOLDER_PERM_STRING = "lockout-fabric.ignored.placeholder";

}
