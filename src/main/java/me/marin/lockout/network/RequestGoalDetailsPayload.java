package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestGoalDetailsPayload(String goalId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestGoalDetailsPayload> ID = new CustomPacketPayload.Type<>(Constants.REQUEST_GOAL_DETAILS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestGoalDetailsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RequestGoalDetailsPayload::goalId,
            RequestGoalDetailsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
