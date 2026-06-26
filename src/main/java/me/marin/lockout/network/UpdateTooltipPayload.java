package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateTooltipPayload(String goal, String tooltip) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateTooltipPayload> ID = new CustomPacketPayload.Type<>(Constants.UPDATE_TOOLTIP);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTooltipPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8,
            UpdateTooltipPayload::goal,
            ByteBufCodecs.STRING_UTF8,
            UpdateTooltipPayload::tooltip,
            UpdateTooltipPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
