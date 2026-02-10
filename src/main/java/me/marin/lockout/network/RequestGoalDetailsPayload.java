package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RequestGoalDetailsPayload(String goalId) implements CustomPayload {
    public static final Id<RequestGoalDetailsPayload> ID = new Id<>(Constants.REQUEST_GOAL_DETAILS_PACKET);
    public static final PacketCodec<RegistryByteBuf, RequestGoalDetailsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            RequestGoalDetailsPayload::goalId,
            RequestGoalDetailsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
