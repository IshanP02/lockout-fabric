package me.marin.lockout.network;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutInitializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockoutVersionPayload(String version) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LockoutVersionPayload> ID = new CustomPacketPayload.Type<>(Constants.LOCKOUT_VERSION_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, LockoutVersionPayload> CODEC = StreamCodec.composite(
            StreamCodec.of((buf, version) -> buf.writeUtf(LockoutInitializer.MOD_VERSION.getFriendlyString()), buf -> buf.readUtf()),
            LockoutVersionPayload::version,
            LockoutVersionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
