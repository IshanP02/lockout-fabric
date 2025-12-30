package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record BroadcastPickBanPayload(String playerName, String goalId, String action) implements CustomPayload {
    public static final Id<BroadcastPickBanPayload> ID = new Id<>(Constants.BROADCAST_PICK_BAN_PACKET);
    public static final PacketCodec<RegistryByteBuf, BroadcastPickBanPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            BroadcastPickBanPayload::playerName,
            PacketCodecs.STRING,
            BroadcastPickBanPayload::goalId,
            PacketCodecs.STRING,
            BroadcastPickBanPayload::action,
            BroadcastPickBanPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
