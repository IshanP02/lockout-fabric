package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GoalDetailsPayload(String goalId, String details) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GoalDetailsPayload> ID = new CustomPacketPayload.Type<>(Constants.GOAL_DETAILS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, GoalDetailsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            GoalDetailsPayload::goalId,
            ByteBufCodecs.STRING_UTF8,
            GoalDetailsPayload::details,
            GoalDetailsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
