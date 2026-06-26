package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StartPickBanSessionPayload(String team1Name, String team2Name, int selectionLimit) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartPickBanSessionPayload> ID = new CustomPacketPayload.Type<>(Constants.START_PICK_BAN_SESSION_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, StartPickBanSessionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            StartPickBanSessionPayload::team1Name,
            ByteBufCodecs.STRING_UTF8,
            StartPickBanSessionPayload::team2Name,
            ByteBufCodecs.INT,
            StartPickBanSessionPayload::selectionLimit,
            StartPickBanSessionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
