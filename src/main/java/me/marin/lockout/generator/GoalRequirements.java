package me.marin.lockout.generator;

import me.marin.lockout.LocateData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.minecraft.world.level.biome.Biomes.*;
import static net.minecraft.world.level.levelgen.structure.BuiltinStructures.*;

public abstract class GoalRequirements {

    public static final GoalRequirements VILLAGE = new Builder()
            .structures(List.of(VILLAGE_DESERT, VILLAGE_PLAINS, VILLAGE_SAVANNA, VILLAGE_SNOWY, VILLAGE_TAIGA))
            .build();
    public static final GoalRequirements OCEAN_MONUMENT = new Builder()
            .structures(List.of(BuiltinStructures.OCEAN_MONUMENT))
            .build();
    public static final GoalRequirements JUNGLE_BIOMES = new Builder()
            .biomes(List.of(BAMBOO_JUNGLE, JUNGLE, SPARSE_JUNGLE))
            .build();
    public static final GoalRequirements SNOWY_BIOMES = new Builder()
            .biomes(List.of(SNOWY_PLAINS, SNOWY_TAIGA, ICE_SPIKES, GROVE, SNOWY_SLOPES, FROZEN_PEAKS, FROZEN_RIVER, SNOWY_BEACH, FROZEN_OCEAN, DEEP_FROZEN_OCEAN))
            .build();
    public static final GoalRequirements RABBIT_BIOMES = new Builder()
            .biomes(List.of(DESERT, SNOWY_PLAINS, SNOWY_TAIGA, GROVE, SNOWY_SLOPES, FLOWER_FOREST, TAIGA, MEADOW, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, CHERRY_GROVE))
            .build();
    public static final GoalRequirements TEAMS_GOAL = new Builder().isTeamSizeOk((size) -> size >= 2).build();
    public static final GoalRequirements TO2_ONLY_GOAL = new Builder().isTeamSizeOk((size) -> size == 2).build();
    public static final GoalRequirements TO2_ONLY_GOAL_NOT_IN_RANDOM_POOL = new Builder().isTeamSizeOk((size) -> size == 2).partOfRandomPool(false).build();
    public static final GoalRequirements NOT_IN_RANDOM_POOL = new Builder().partOfRandomPool(false).build();

    private GoalRequirements() {}

    /**
     * At least one of these biomes needs to be close to spawn. Keys can be found in {@link BiomeKeys}.
     */
    public List<ResourceKey<Biome>> getRequiredBiomes() {
        return Collections.emptyList();
    }


    /**
     * At least one of these structures needs to be close to spawn. Keys can be found in {@link StructureKeys}.
     */
    public List<ResourceKey<Structure>> getRequiredStructures() {
        return Collections.emptyList();
    }

    public boolean isPartOfRandomPool() {
        return true;
    }

    public boolean isTeamsSizeOk(int teamsSize) {
        return true;
    }


    public final boolean isSatisfied(Map<ResourceKey<Biome>, LocateData> biomes, Map<ResourceKey<Structure>, LocateData> structures) {
        boolean hasRequiredBiome = true;
        if (getRequiredBiomes() != null) {
            for (ResourceKey<Biome> biome : getRequiredBiomes()) {
                LocateData locateData = biomes.get(biome);
                if (locateData != null && locateData.wasLocated()) {
                    hasRequiredBiome = true;
                    break;
                } else {
                    hasRequiredBiome = false;
                }
            }
        }


        boolean hasRequiredStructure = true;
        if (getRequiredStructures() != null) {
            for (ResourceKey<Structure> structure : getRequiredStructures()) {
                LocateData locateData = structures.get(structure);
                if (locateData != null && locateData.wasLocated()) {
                    hasRequiredStructure = true;
                    break;
                } else {
                    hasRequiredStructure = false;
                }
            }
        }

        return hasRequiredBiome && hasRequiredStructure;
    }

    public static class Builder {

        private List<ResourceKey<Biome>> biomes = Collections.emptyList();
        private List<ResourceKey<Structure>> structures = Collections.emptyList();
        private boolean partOfRandomPool = true;
        private Function<Integer, Boolean> isTeamSizeOk = (size) -> true;

        public Builder biomes(List<ResourceKey<Biome>> biomes) {
            this.biomes = biomes;
            return this;
        }
        public Builder structures(List<ResourceKey<Structure>> structures) {
            this.structures = structures;
            return this;
        }
        public Builder partOfRandomPool(boolean partOfRandomPool) {
            this.partOfRandomPool = partOfRandomPool;
            return this;
        }
        public Builder isTeamSizeOk(Function<Integer, Boolean> isTeamSizeOk) {
            this.isTeamSizeOk = isTeamSizeOk;
            return this;
        }

        public GoalRequirements build() {
            return new GoalRequirements() {
                @Override
                public List<ResourceKey<Biome>> getRequiredBiomes() {
                    return biomes;
                }

                @Override
                public List<ResourceKey<Structure>> getRequiredStructures() {
                    return structures;
                }

                @Override
                public boolean isPartOfRandomPool() {
                    return partOfRandomPool;
                }

                @Override
                public boolean isTeamsSizeOk(int teamsSize) {
                    return isTeamSizeOk.apply(teamsSize);
                }
            };
        }

    }

}
