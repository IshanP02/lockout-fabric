package me.marin.lockout.type;

import java.util.List;

import me.marin.lockout.generator.GoalGroup;

public enum BoardType {
    EASY(List.of(GoalGroup.KILL_MOB, GoalGroup.EFFECT)), // Exclude these groups
    TOURNAMENT(List.of(GoalGroup.BREED, GoalGroup.EAT_FOOD)); // Exclude these groups

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