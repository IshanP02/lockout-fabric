package me.marin.lockout.client;

import me.marin.lockout.LocateData;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.generator.GoalRequirements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ClientLocateUtil {

    private static final Map<RegistryKey<Biome>, LocateData> CACHED_BIOMES = new HashMap<>();
    private static final Map<RegistryKey<Structure>, LocateData> CACHED_STRUCTURES = new HashMap<>();
    private static boolean CACHE_BUILT = false;
    
    // Server-provided locate data (for multiplayer)
    private static final Map<RegistryKey<Biome>, LocateData> SERVER_BIOMES = new HashMap<>();
    private static final Map<RegistryKey<Structure>, LocateData> SERVER_STRUCTURES = new HashMap<>();
    private static boolean HAS_SERVER_DATA = false;

    /**
     * Update the cached locate data with server-provided data
     */
    public static void setServerLocateData(Map<RegistryKey<Biome>, LocateData> biomes, Map<RegistryKey<Structure>, LocateData> structures) {
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
    public static void buildCacheFromRegisteredGoals(MinecraftClient client) {
        if (client == null || client.world == null) return;
        try {
            Set<RegistryKey<Biome>> biomeKeys = new HashSet<>();
            Set<RegistryKey<Structure>> structureKeys = new HashSet<>();
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

    private static Map<RegistryKey<Biome>, LocateData> locateBiomesImpl(MinecraftClient client, Iterable<RegistryKey<Biome>> biomesToCheck) {
        Map<RegistryKey<Biome>, LocateData> map = new HashMap<>();
        if (client == null || client.world == null) return map;

        try {
            var server = client.getServer();
            if (server == null) return map;

            BlockPos currentPos = new BlockPos(0, 64, 0);
            for (RegistryKey<Biome> biome : biomesToCheck) {
                var pair = server.getOverworld().locateBiome(
                        biomeRegistryEntry -> biomeRegistryEntry.matchesKey(biome),
                        currentPos,
                        LockoutServer.LOCATE_SEARCH,
                        32,
                        64);
                LocateData data = new LocateData(false, 0);
                if (pair != null) {
                    int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
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

    private static Map<RegistryKey<Structure>, LocateData> locateStructuresImpl(MinecraftClient client, Iterable<RegistryKey<Structure>> structuresToCheck) {
        Map<RegistryKey<Structure>, LocateData> map = new HashMap<>();
        if (client == null || client.world == null) return map;

        try {
            var server = client.getServer();
            if (server == null) return map;

            BlockPos currentPos = new BlockPos(0, 64, 0);
            Registry<Structure> registry = server.getOverworld().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
            for (RegistryKey<Structure> structure : structuresToCheck) {
                RegistryEntryList<Structure> structureList = RegistryEntryList.of(registry.getOrThrow(structure));
                var pair = server.getOverworld().getChunkManager().getChunkGenerator().locateStructure(
                        server.getOverworld(),
                        structureList,
                        currentPos,
                        LockoutServer.LOCATE_SEARCH,
                        false);

                LocateData data = new LocateData(false, 0);
                if (pair != null) {
                    int distance = MathHelper.floor(LocateCommand.getDistance(currentPos.getX(), currentPos.getZ(), pair.getFirst().getX(), pair.getFirst().getZ()));
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

    public static Map<RegistryKey<Biome>, LocateData> locateBiomes(MinecraftClient client, Iterable<RegistryKey<Biome>> biomesToCheck) {
        // Prefer server-provided data if available (multiplayer)
        if (HAS_SERVER_DATA) {
            Map<RegistryKey<Biome>, LocateData> result = new HashMap<>();
            for (RegistryKey<Biome> k : biomesToCheck) {
                LocateData d = SERVER_BIOMES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        
        // Fall back to cached or local locate (singleplayer)
        if (CACHE_BUILT) {
            Map<RegistryKey<Biome>, LocateData> result = new HashMap<>();
            for (RegistryKey<Biome> k : biomesToCheck) {
                LocateData d = CACHED_BIOMES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        return locateBiomesImpl(client, biomesToCheck);
    }

    public static Map<RegistryKey<Structure>, LocateData> locateStructures(MinecraftClient client, Iterable<RegistryKey<Structure>> structuresToCheck) {
        // Prefer server-provided data if available (multiplayer)
        if (HAS_SERVER_DATA) {
            Map<RegistryKey<Structure>, LocateData> result = new HashMap<>();
            for (RegistryKey<Structure> k : structuresToCheck) {
                LocateData d = SERVER_STRUCTURES.get(k);
                if (d != null) result.put(k, d);
            }
            return result;
        }
        
        // Fall back to cached or local locate (singleplayer)
        if (CACHE_BUILT) {
            Map<RegistryKey<Structure>, LocateData> result = new HashMap<>();
            for (RegistryKey<Structure> k : structuresToCheck) {
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
