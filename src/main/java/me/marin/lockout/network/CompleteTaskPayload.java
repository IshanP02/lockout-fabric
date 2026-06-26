package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CompleteTaskPayload(String goal, int teamIndex, String completionMessage) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompleteTaskPayload> ID = new CustomPacketPayload.Type<>(Constants.COMPLETE_TASK_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompleteTaskPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CompleteTaskPayload::goal,
            ByteBufCodecs.INT,
            CompleteTaskPayload::teamIndex,
            ByteBufCodecs.STRING_UTF8,
            CompleteTaskPayload::completionMessage,
            CompleteTaskPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
