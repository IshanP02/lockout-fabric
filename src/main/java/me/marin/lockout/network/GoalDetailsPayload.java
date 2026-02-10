package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record GoalDetailsPayload(String goalId, String details) implements CustomPayload {
    public static final Id<GoalDetailsPayload> ID = new Id<>(Constants.GOAL_DETAILS_PACKET);
    public static final PacketCodec<RegistryByteBuf, GoalDetailsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            GoalDetailsPayload::goalId,
            PacketCodecs.STRING,
            GoalDetailsPayload::details,
            GoalDetailsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
