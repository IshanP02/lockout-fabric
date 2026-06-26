package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.generator.GoalRequirements;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.Structure;

import static me.marin.lockout.server.LockoutServer.*;

public class ServerStartedEventHandler implements ServerLifecycleEvents.ServerStarted {

    @Override
    public void onServerStarted(MinecraftServer server) {
        server.execute(() -> {
            Lockout.log("Locating all required Structures and Biomes");
            LockoutServer.server = server;
            long start = System.currentTimeMillis();

            AVAILABLE_DYE_COLORS.add(DyeColor.BLACK);
            AVAILABLE_DYE_COLORS.add(DyeColor.WHITE);
            AVAILABLE_DYE_COLORS.add(DyeColor.GRAY);
            AVAILABLE_DYE_COLORS.add(DyeColor.LIGHT_GRAY);
            AVAILABLE_DYE_COLORS.add(DyeColor.BLUE);
            AVAILABLE_DYE_COLORS.add(DyeColor.LIGHT_BLUE);
            AVAILABLE_DYE_COLORS.add(DyeColor.ORANGE);
            AVAILABLE_DYE_COLORS.add(DyeColor.RED);
            AVAILABLE_DYE_COLORS.add(DyeColor.YELLOW);
            AVAILABLE_DYE_COLORS.add(DyeColor.MAGENTA);
            AVAILABLE_DYE_COLORS.add(DyeColor.PINK);
            AVAILABLE_DYE_COLORS.add(DyeColor.PURPLE);

            boolean hasCactus = locateBiome(server, Biomes.DESERT).wasLocated();
            hasCactus |= locateBiome(server, Biomes.BADLANDS).wasLocated();
            hasCactus |= locateBiome(server, Biomes.ERODED_BADLANDS).wasLocated();
            hasCactus |= locateBiome(server, Biomes.WOODED_BADLANDS).wasLocated();
            if (hasCactus) {
                AVAILABLE_DYE_COLORS.add(DyeColor.GREEN);
                AVAILABLE_DYE_COLORS.add(DyeColor.LIME);
                AVAILABLE_DYE_COLORS.add(DyeColor.CYAN);
            } else {
                if (locateBiome(server, Biomes.WARM_OCEAN).wasLocated()) {
                    AVAILABLE_DYE_COLORS.add(DyeColor.LIME);
                }
            }

            boolean hasCocoaBeans;
            hasCocoaBeans  = locateBiome(server, Biomes.JUNGLE).wasLocated();
            hasCocoaBeans |= locateBiome(server, Biomes.BAMBOO_JUNGLE).wasLocated();
            hasCocoaBeans |= locateBiome(server, Biomes.JUNGLE).wasLocated();
            if (hasCocoaBeans) {
                AVAILABLE_DYE_COLORS.add(DyeColor.BROWN);
            }

            for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
                GoalRequirements goalRequirements = GoalRegistry.INSTANCE.getGoalGenerator(id);
                if (goalRequirements == null) continue;

                for (ResourceKey<Biome> biome : goalRequirements.getRequiredBiomes()) {
                    locateBiome(server, biome);
                }

                for (ResourceKey<Structure> structure : goalRequirements.getRequiredStructures()) {
                    locateStructure(server, structure);
                }
            }
            long end = System.currentTimeMillis();
            Lockout.log("Located " + BIOME_LOCATE_DATA.size() + " biomes and " + STRUCTURE_LOCATE_DATA.size() + " structures in " + String.format("%.2f", ((end-start)/1000.0)) + "s!");

            // Freeze ticks until lockout/blackout game starts
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");

            // Send locate data to all connected clients
            me.marin.lockout.network.SyncLocateDataPayload payload = new me.marin.lockout.network.SyncLocateDataPayload(
                new java.util.HashMap<>(BIOME_LOCATE_DATA),
                new java.util.HashMap<>(STRUCTURE_LOCATE_DATA)
            );
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        });
    }
}
