package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateTimerPayload(long ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateTimerPayload> ID = new CustomPacketPayload.Type<>(Constants.UPDATE_TIMER_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTimerPayload> CODEC = StreamCodec.composite(ByteBufCodecs.LONG, UpdateTimerPayload::ticks, UpdateTimerPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
