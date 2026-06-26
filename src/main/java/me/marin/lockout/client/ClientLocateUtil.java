package me.marin.lockout.client;

import me.marin.lockout.LocateData;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.generator.GoalRequirements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ClientLocateUtil {

    private static final Map<ResourceKey<Biome>, LocateData> CACHED_BIOMES = new HashMap<>();
    private static final Map<ResourceKey<Structure>, LocateData> CACHED_STRUCTURES = new HashMap<>();
    private static boolean CACHE_BUILT = false;
    
    // Server-provided locate data (for multiplayer)
    private static final Map<ResourceKey<Biome>, LocateData> SERVER_BIOMES = new HashMap<>();
    private static final Map<ResourceKey<Structure>, LocateData> SERVER_STRUCTURES = new HashMap<>();
    private static boolean HAS_SERVER_DATA = false;

    /**
     * Update the cached locate data with server-provided data
     */
    public static void setServerLocateData(Map<ResourceKey<Biome>, LocateData> biomes, Map<ResourceKey<Structure>, LocateData> structures) {
        SERVER_BIOMES.clear();
        SERVER_BIOMES.putAll(biomes);
        SERVER_STRUCTURES.clear();
        SERVER_STRUCTURES.putAll(structures);
        HAS_SERVER_DATA = true;
    }

    /**
     * Build the locate cache once at world load. Call this when the client world finishes loading.
     * It will collect all required biomes/structures from registered goals and run locate once.
     */
    public static void buildCacheFromRegisteredGoals(Minecraft client) {
        if (client == null || client.level == null) return;
        try {
            Set<ResourceKey<Biome>> biomeKeys = new HashSet<>();
            Set<ResourceKey<Structure>> structureKeys = new HashSet<>();
            for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
                GoalRequirements req = GoalRegistry.INSTANCE.getGoalGenerator(id);
                if (req == null) continue;
                if (req.getRequiredBiomes() != null) biomeKeys.addAll(req.getRequiredBiomes());
                if (req.getRequiredStructures() != null) structureKeys.addAll(req.getRequiredStructures());
            }

            CACHED_BIOMES.clear();
            CACHED_BIOMES.putAll(locateBiomesImpl(client, biomeKeys));

            CACHED_STRUCTURES.clear();
            CACHED_STRUCTURES.putAll(locateStructuresImpl(client, structureKeys));

            CACHE_BUILT = true;
        } catch (Throwable t) {
            // ignore
        }
    }

    private static Map<ResourceKey<Biome>, LocateData> locateBiomesImpl(Minecraft client, Iterable<ResourceKey<Biome>> biomesToCheck) {
        Map<ResourceKey<Biome>, LocateData> map = new HashMap<>();
        if (client == null || client.level == null) return map;

        try {
            var server = client.getSingleplayerServer();
            if (server == null) return map;

            BlockPos currentPos = new BlockPos(0, 64, 0);
            for (ResourceKey<Biome> biome : biomesToCheck) {
                var chunkSrc = server.overworld().getChunkSource();
                var pair = chunkSrc.getGenerator().getBiomeSource().findClosestBiome3d(
                        currentPos,
                        LockoutServer.LOCATE_SEARCH,
                        32,
                        64,
                        biomeHolder -> biomeHolder.is(biome),
                        chunkSrc.randomState().sampler(),
                        server.overworld());
                LocateData data = new LocateData(false, 0);
                if (pair != null) {
                    int distance = Mth.floor(Math.sqrt(Math.pow(pair.getFirst().getX() - currentPos.getX(), 2) + Math.pow(pair.getFirst().getZ() - currentPos.getZ(), 2)));
                    if (distance < LockoutServer.LOCATE_SEARCH) {
                        data = new LocateData(true, distance);
                    }
                }
                map.put(biome, data);
            }
        } catch (Throwable t) {
            // If locating fails on client (e.g., multiplayer restrictions), return empty map
        }

        return map;
    }

    private static Map<ResourceKey<Structure>, LocateData> locateStructuresImpl(Minecraft client, Iterable<ResourceKey<Structure>> structuresToCheck) {
        Map<ResourceKey<Structure>, LocateData> map = new HashMap<>();
        if (client == null || client.level == null) return map;

        try {
            var server = client.getSingleplayerServer();
            if (server == null) return map;

            BlockPos currentPos = new BlockPos(0, 64, 0);
            Registry<Structure> registry = server.overworld().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (ResourceKey<Structure> structure : structuresToCheck) {
                HolderSet<Structure> structureList = HolderSet.direct(registry.getOrThrow(structure));
                var pair = server.overworld().getChunkSource().getGenerator().findNearestMapStructure(
                        server.overworld(),
                        structureList,
                        currentPos,
                        LockoutServer.LOCATE_SEARCH,
                        false);

                LocateData data = new LocateData(false, 0);
                if (pair != null) {
                    int distance = Mth.floor(Math.sqrt(Math.pow(pair.getFirst().getX() - currentPos.getX(), 2) + Math.pow(pair.getFirst().getZ() - currentPos.getZ(), 2)));
                    if (distance < LockoutServer.LOCATE_SEARCH) {
                        data = new LocateData(true, distance);
                    }
                }
                map.put(structure, data);
            }
        } catch (Throwable t) {
            // ignore
        }

        return map;
    }

    public static Map<ResourceKey<Biome>, LocateData> locateBiomes(Minecraft client, Iterable<ResourceKey<Biome>> biomesToCheck) {
        // Prefer server-provided data if available (multiplayer)
        if (HAS_SERVER_DATA) {
            Map<ResourceKey<Biome>, LocateData> result = new HashMap<>();
            for (ResourceKey<Biome> k : biomesToCheck) {
                LocateData d = SERVER_BIOMES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        
        // Fall back to cached or local locate (singleplayer)
        if (CACHE_BUILT) {
            Map<ResourceKey<Biome>, LocateData> result = new HashMap<>();
            for (ResourceKey<Biome> k : biomesToCheck) {
                LocateData d = CACHED_BIOMES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        return locateBiomesImpl(client, biomesToCheck);
    }

    public static Map<ResourceKey<Structure>, LocateData> locateStructures(Minecraft client, Iterable<ResourceKey<Structure>> structuresToCheck) {
        // Prefer server-provided data if available (multiplayer)
        if (HAS_SERVER_DATA) {
            Map<ResourceKey<Structure>, LocateData> result = new HashMap<>();
            for (ResourceKey<Structure> k : structuresToCheck) {
                LocateData d = SERVER_STRUCTURES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        
        // Fall back to cached or local locate (singleplayer)
        if (CACHE_BUILT) {
            Map<ResourceKey<Structure>, LocateData> result = new HashMap<>();
            for (ResourceKey<Structure> k : structuresToCheck) {
                LocateData d = CACHED_STRUCTURES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        return locateStructuresImpl(client, structuresToCheck);
    }

    /**
     * Clear any cached locate data. Call on disconnect or when world unloads.
     */
    public static void clearCache() {
        CACHED_BIOMES.clear();
        CACHED_STRUCTURES.clear();
        CACHE_BUILT = false;
        SERVER_BIOMES.clear();
        SERVER_STRUCTURES.clear();
        HAS_SERVER_DATA = false;
    }
}
