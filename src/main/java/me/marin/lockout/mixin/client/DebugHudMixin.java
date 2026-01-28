package me.marin.lockout.mixin.client;

import me.marin.lockout.LockoutConfig;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @Shadow @Final
    private MinecraftClient client;

    @Shadow
    private ChunkPos pos;

    // HEIGHT_MAP_TYPES removed — not present in 1.21+ DebugHud

    @Shadow
    private void drawText(DrawContext context, List<String> text, boolean left) {}

    @Shadow
    public void resetChunk() {}

    @Shadow
    private WorldChunk getClientChunk() {
        throw new AbstractMethodError("Shadow");
    }

    // Removed @Shadow getBiomeString — not present in current DebugHud mappings.
    @Unique
    private static String getBiomeStringLocal(RegistryEntry<Biome> biome) {
        if (biome == null) return "unknown";
        try {
            // Prefer readable id if available
            try {
                java.lang.reflect.Method rk = biome.getClass().getMethod("registryKey");
                Object key = rk.invoke(biome);
                if (key != null) {
                    try {
                        java.lang.reflect.Method val = key.getClass().getMethod("getValue");
                        Object id = val.invoke(key);
                        if (id != null) return id.toString();
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (NoSuchMethodException ignored) {}

            // Fallback to biome value's toString
            return String.valueOf(biome.value());
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Shadow
    private ServerWorld getServerWorld() { throw new AbstractMethodError("Shadow"); }

    @Shadow
    private WorldChunk getChunk() { throw new AbstractMethodError("Shadow"); }

    @Unique
    private final DebugHud INSTANCE = (DebugHud) (Object) this;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lockout$render(DrawContext context, CallbackInfo ci) {
        drawLockoutDebug(context);
        ci.cancel();
    }

    @Unique
    private void drawLockoutDebug(DrawContext context) {
        // Defensive guards: render() can be called early where client or camera entity is null.
        if (this.client == null || this.client.world == null) return;

        Entity entity = this.client.getCameraEntity();
        if (entity == null) return;

        List<String> text = new ArrayList<>();

        BlockPos blockPos = entity.getBlockPos();
        Direction direction = entity.getHorizontalFacing();

        String directionString = switch (direction) {
            case NORTH -> "Towards negative Z";
            case SOUTH -> "Towards positive Z";
            case WEST -> "Towards negative X";
            case EAST -> "Towards positive X";
            default -> "Invalid";
        };

        ChunkPos chunkPos = new ChunkPos(blockPos);
        if (!Objects.equals(this.pos, chunkPos)) {
            this.pos = chunkPos;
            this.resetChunk();
        }

        text.add("Minecraft " + SharedConstants.getGameVersion().name() + " (" + this.client.getGameVersion() + "/" + ClientBrandRetriever.getClientModName() + ("release".equalsIgnoreCase(this.client.getVersionType()) ? "" : "/" + this.client.getVersionType()) + ")");
        text.add("");
        if (this.client.worldRenderer != null) {
            text.add(this.client.worldRenderer.getChunksDebugString()); // C value
            text.add(this.client.worldRenderer.getEntitiesDebugString()); // E value
        }
        text.add("");
        text.add(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", this.client.getCameraEntity().getX(), this.client.getCameraEntity().getY(), this.client.getCameraEntity().getZ()));
        text.add(String.format(Locale.ROOT, "Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        text.add(String.format(Locale.ROOT, "Chunk: %d %d %d in %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15, chunkPos.x, ChunkSectionPos.getSectionCoord(blockPos.getY()), chunkPos.z));
        text.add(String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, directionString, MathHelper.wrapDegrees(entity.getYaw()), MathHelper.wrapDegrees(entity.getPitch())));

        WorldChunk worldChunk = this.getClientChunk();
        if (worldChunk.isEmpty()) {
            text.add("Waiting for chunk...");
        } else {
            WorldChunk wc = this.getChunk();
            StringBuilder sb = new StringBuilder("SH");

            for (Heightmap.Type type : Heightmap.Type.values()) {
                if (!type.isStoredServerSide()) continue;

                sb.append(" ")
                  .append(type.name())
                  .append(": ");

                if (wc != null) {
                    sb.append(wc.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
                } else {
                    sb.append("??");
                }
            }

            text.add(sb.toString());

            RegistryEntry<Biome> var27 = this.client.world.getBiome(blockPos);
            text.add("Biome: " + getBiomeStringLocal(var27));
        }

        if (LockoutConfig.getInstance().showNoiseRouterLine) {
            ServerWorld serverWorld = this.getServerWorld();
            if (serverWorld != null) {
                ServerChunkManager serverChunkManager = serverWorld.getChunkManager();
                ChunkGenerator chunkGenerator = serverChunkManager.getChunkGenerator();
                NoiseConfig noiseConfig = serverChunkManager.getNoiseConfig();
                chunkGenerator.appendDebugHudText(text, noiseConfig, blockPos);
            }
        }

        // Targeted block/fluid/property debug info removed for current mappings compatibility.

        entity = this.client.targetedEntity;
        if (entity != null) {
            text.add("");
            text.add(Formatting.UNDERLINE + "Targeted Entity");
            text.add(String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType())));
        }

        this.drawText(context, text, true);
    }

    // Removed drawRightText injection; render() is used instead for 1.21+.

}
