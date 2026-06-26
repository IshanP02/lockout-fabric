package me.marin.lockout.network;

import me.marin.lockout.Constants;
import me.marin.lockout.LocateData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.Map;

public record SyncLocateDataPayload(
        Map<ResourceKey<Biome>, LocateData> biomeLocateData,
        Map<ResourceKey<Structure>, LocateData> structureLocateData
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncLocateDataPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "sync_locate_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLocateDataPayload> CODEC = new StreamCodec<>() {
        @Override
        public SyncLocateDataPayload decode(RegistryFriendlyByteBuf buf) {
            // Read biome locate data
            int biomeSize = buf.readInt();
            Map<ResourceKey<Biome>, LocateData> biomeMap = new HashMap<>();
            for (int i = 0; i < biomeSize; i++) {
                Identifier biomeId = buf.readIdentifier();
                boolean wasLocated = buf.readBoolean();
                int distance = buf.readInt();
                biomeMap.put(ResourceKey.create(Registries.BIOME, biomeId), new LocateData(wasLocated, distance));
            }

            // Read structure locate data
            int structureSize = buf.readInt();
            Map<ResourceKey<Structure>, LocateData> structureMap = new HashMap<>();
            for (int i = 0; i < structureSize; i++) {
                Identifier structureId = buf.readIdentifier();
                boolean wasLocated = buf.readBoolean();
                int distance = buf.readInt();
                structureMap.put(ResourceKey.create(Registries.STRUCTURE, structureId), new LocateData(wasLocated, distance));
            }

            return new SyncLocateDataPayload(biomeMap, structureMap);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncLocateDataPayload payload) {
            // Write biome locate data
            buf.writeInt(payload.biomeLocateData().size());
            for (Map.Entry<ResourceKey<Biome>, LocateData> entry : payload.biomeLocateData().entrySet()) {
                buf.writeIdentifier(entry.getKey().identifier());
                buf.writeBoolean(entry.getValue().wasLocated());
                buf.writeInt(entry.getValue().distance());
            }

            // Write structure locate data
            buf.writeInt(payload.structureLocateData().size());
            for (Map.Entry<ResourceKey<Structure>, LocateData> entry : payload.structureLocateData().entrySet()) {
                buf.writeIdentifier(entry.getKey().identifier());
                buf.writeBoolean(entry.getValue().wasLocated());
                buf.writeInt(entry.getValue().distance());
            }
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
