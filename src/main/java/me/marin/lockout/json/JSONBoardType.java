package me.marin.lockout.json;

import java.util.List;

/**
 * Represents a custom BoardType for JSON serialization.
 * Contains a list of goal IDs that should be excluded from board generation.
 */
public class JSONBoardType {

    public String name;

    //List of goal IDs (from GoalType constants) that should be excluded when using this BoardType
    public List<String> excludedGoals;

    //Optional description of this BoardType
    public String description;
}