package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class StartLockoutPayload implements CustomPacketPayload {
    public static final StartLockoutPayload INSTANCE = new StartLockoutPayload();
    public static final CustomPacketPayload.Type<StartLockoutPayload> ID = new CustomPacketPayload.Type<>(Constants.START_LOCKOUT_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, StartLockoutPayload> CODEC = StreamCodec.unit(INSTANCE);

    private StartLockoutPayload() {}

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
