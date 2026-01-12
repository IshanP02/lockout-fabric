package me.marin.lockout.network;

import me.marin.lockout.Constants;
import me.marin.lockout.LocateData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;

public record SyncLocateDataPayload(
        Map<RegistryKey<Biome>, LocateData> biomeLocateData,
        Map<RegistryKey<Structure>, LocateData> structureLocateData
) implements CustomPayload {
    public static final Id<SyncLocateDataPayload> ID = new Id<>(Identifier.of(Constants.NAMESPACE, "sync_locate_data"));
    public static final PacketCodec<RegistryByteBuf, SyncLocateDataPayload> CODEC = new PacketCodec<>() {
        @Override
        public SyncLocateDataPayload decode(RegistryByteBuf buf) {
            // Read biome locate data
            int biomeSize = buf.readInt();
            Map<RegistryKey<Biome>, LocateData> biomeMap = new HashMap<>();
            for (int i = 0; i < biomeSize; i++) {
                Identifier biomeId = buf.readIdentifier();
                boolean wasLocated = buf.readBoolean();
                int distance = buf.readInt();
                biomeMap.put(RegistryKey.of(RegistryKeys.BIOME, biomeId), new LocateData(wasLocated, distance));
            }

            // Read structure locate data
            int structureSize = buf.readInt();
            Map<RegistryKey<Structure>, LocateData> structureMap = new HashMap<>();
            for (int i = 0; i < structureSize; i++) {
                Identifier structureId = buf.readIdentifier();
                boolean wasLocated = buf.readBoolean();
                int distance = buf.readInt();
                structureMap.put(RegistryKey.of(RegistryKeys.STRUCTURE, structureId), new LocateData(wasLocated, distance));
            }

            return new SyncLocateDataPayload(biomeMap, structureMap);
        }

        @Override
        public void encode(RegistryByteBuf buf, SyncLocateDataPayload payload) {
            // Write biome locate data
            buf.writeInt(payload.biomeLocateData().size());
            for (Map.Entry<RegistryKey<Biome>, LocateData> entry : payload.biomeLocateData().entrySet()) {
                buf.writeIdentifier(entry.getKey().getValue());
                buf.writeBoolean(entry.getValue().wasLocated());
                buf.writeInt(entry.getValue().distance());
            }

            // Write structure locate data
            buf.writeInt(payload.structureLocateData().size());
            for (Map.Entry<RegistryKey<Structure>, LocateData> entry : payload.structureLocateData().entrySet()) {
                buf.writeIdentifier(entry.getKey().getValue());
                buf.writeBoolean(entry.getValue().wasLocated());
                buf.writeInt(entry.getValue().distance());
            }
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
