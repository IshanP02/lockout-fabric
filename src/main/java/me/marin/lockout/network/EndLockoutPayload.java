package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EndLockoutPayload(int[] winners, long time) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EndLockoutPayload> ID = new CustomPacketPayload.Type<>(Constants.END_LOCKOUT_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, EndLockoutPayload> CODEC = StreamCodec.composite(
            StreamCodec.<RegistryFriendlyByteBuf, int[]>of(
                    (buf, winners) -> { buf.writeVarInt(winners.length); for (int w : winners) buf.writeVarInt(w); },
                    buf -> { int n = buf.readVarInt(); int[] arr = new int[n]; for (int i = 0; i < n; i++) arr[i] = buf.readVarInt(); return arr; }),
            EndLockoutPayload::winners,
            ByteBufCodecs.LONG,
            EndLockoutPayload::time,
            EndLockoutPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
