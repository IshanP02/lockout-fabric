package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BroadcastPickBanPayload(String playerName, String goalId, String action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BroadcastPickBanPayload> ID = new CustomPacketPayload.Type<>(Constants.BROADCAST_PICK_BAN_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, BroadcastPickBanPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BroadcastPickBanPayload::playerName,
            ByteBufCodecs.STRING_UTF8,
            BroadcastPickBanPayload::goalId,
            ByteBufCodecs.STRING_UTF8,
            BroadcastPickBanPayload::action,
            BroadcastPickBanPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
