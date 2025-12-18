package me.marin.lockout.type;

import java.util.List;

import me.marin.lockout.generator.GoalGroup;

public enum BoardType {
    EASY(List.of(GoalGroup.BANNEDGOALS, GoalGroup.HONEY, GoalGroup.STRIDER, GoalGroup.END, GoalGroup.KILL_UNIQUE_HOSTILES)), // Exclude these groups
    TOURNAMENT(List.of(GoalGroup.BANNEDGOALS)); // Exclude these groups

    private final List<GoalGroup> excludedGroups;

    BoardType(List<GoalGroup> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public List<GoalGroup> getExcludedGroups() {
        return excludedGroups;
    }

    public boolean isGoalExcluded(String goal) {
        return excludedGroups.stream().anyMatch(group -> group.containsGoal(goal));
    }
}