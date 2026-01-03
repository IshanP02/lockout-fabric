package me.marin.lockout.generator;

import me.marin.lockout.LocateData;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.type.BoardTypeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import oshi.util.tuples.Pair;

import java.util.*;

public class BoardGenerator {

    private final List<String> registeredGoals;
    private final List<LockoutTeamServer> teams;
    private final List<DyeColor> attainableDyes;
    private final Map<RegistryKey<Biome>, LocateData> biomes;
    private final Map<RegistryKey<Structure>, LocateData> structures;

    public BoardGenerator(List<String> registeredGoals, List<LockoutTeamServer> teams, List<DyeColor> attainableDyes, Map<RegistryKey<Biome>, LocateData> biomes, Map<RegistryKey<Structure>, LocateData> structures) {
        this.registeredGoals = registeredGoals;
        this.teams = teams;
        this.attainableDyes = attainableDyes;
        this.biomes = biomes;
        this.structures = structures;
    }

    public LockoutBoard generateBoard(int size, String boardTypeName) {
        // Prepare a mutable list of available goals and remove banned goals
        List<String> availableGoals = new ArrayList<>(registeredGoals);
        
        // Combine BANS and PENDING_BANS for board generation
        List<String> banned = new ArrayList<>();
        banned.addAll(GoalGroup.BANS.getGoals());
        banned.addAll(GoalGroup.PENDING_BANS.getGoals());
        
        if (!banned.isEmpty()) {
            availableGoals.removeAll(banned);
        }

        Collections.shuffle(availableGoals);

        List<Pair<String, String>> goals = new ArrayList<>();
        List<String> goalTypes = new ArrayList<>();

        // Combine PICKS and PENDING_PICKS for board generation
        List<String> picks = new ArrayList<>();
        picks.addAll(GoalGroup.PICKS.getGoals());
        picks.addAll(GoalGroup.PENDING_PICKS.getGoals());
        
        if (!picks.isEmpty()) {
            for (String pick : picks) {
                if (goals.size() >= size * size) break;
                // Only add if registered and not already added, and not banned
                if (!registeredGoals.contains(pick)) continue;
                if (banned != null && banned.contains(pick)) continue;
                if (goalTypes.contains(pick)) continue;

                Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(pick);
                String data = gen.map(g -> g.generateData(attainableDyes)).orElse(GoalDataConstants.DATA_NONE);
                goals.add(new Pair<>(pick, data));
                goalTypes.add(pick);

                // Ensure we don't pick it again from the pool
                availableGoals.remove(pick);
            }
        }

        // Fill remaining slots from the available goals, respecting requirements
        ListIterator<String> it = availableGoals.listIterator();
        while (goals.size() < size * size && it.hasNext()) {
            String goal = it.next();

            // Check if the goal should be excluded for this board type
            if (BoardTypeManager.INSTANCE.isGoalExcluded(boardTypeName, goal)) {
                continue;
            }

            if (!GoalGroup.canAdd(goal, goalTypes)) {
                continue;
            }

            GoalRequirements goalRequirements = GoalRegistry.INSTANCE.getGoalGenerator(goal);
            if (goalRequirements != null) {
                if (!goalRequirements.isTeamsSizeOk(teams.size())) {
                    continue;
                }
                if (!goalRequirements.isPartOfRandomPool()) {
                    continue;
                }
                if (!goalRequirements.isSatisfied(biomes, structures)) {
                    continue;
                }
            }

            Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(goal);
            String data = gen.map(g -> g.generateData(attainableDyes)).orElse(GoalDataConstants.DATA_NONE);

            goals.add(new Pair<>(goal, data));
            goalTypes.add(goal);
        }

        if (goals.size() < size * size) {
            return generateBoard(size, boardTypeName);
        }

        // Shuffle the board again. Some goals will always be after some other goals (GoalGroup#requirePredecessor),
        // and shuffle fixes this.
        Collections.shuffle(goals);

        return new LockoutBoard(goals);
    }

}
