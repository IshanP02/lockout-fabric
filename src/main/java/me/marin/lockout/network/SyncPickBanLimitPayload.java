package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SyncPickBanLimitPayload(int limit) implements CustomPayload {
    public static final Id<SyncPickBanLimitPayload> ID = new Id<>(Constants.SYNC_PICK_BAN_LIMIT_PACKET);
    public static final PacketCodec<RegistryByteBuf, SyncPickBanLimitPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            SyncPickBanLimitPayload::limit,
            SyncPickBanLimitPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
