package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncPickBanLimitPayload(int limit) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPickBanLimitPayload> ID = new CustomPacketPayload.Type<>(Constants.SYNC_PICK_BAN_LIMIT_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPickBanLimitPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncPickBanLimitPayload::limit,
            SyncPickBanLimitPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
