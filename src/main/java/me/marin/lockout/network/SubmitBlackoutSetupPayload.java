package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SubmitBlackoutSetupPayload(int boardSize, int startTimerSeconds, String boardTypeName) implements CustomPayload {
    public static final Id<SubmitBlackoutSetupPayload> ID = new Id<>(Constants.SUBMIT_BLACKOUT_SETUP_PACKET);
    public static final PacketCodec<RegistryByteBuf, SubmitBlackoutSetupPayload> CODEC = new PacketCodec<>() {
        @Override
        public SubmitBlackoutSetupPayload decode(RegistryByteBuf buf) {
            int boardSize = buf.readVarInt();
            int startTimerSeconds = buf.readVarInt();
            String boardTypeName = buf.readString();
            return new SubmitBlackoutSetupPayload(boardSize, startTimerSeconds, boardTypeName);
        }

        @Override
        public void encode(RegistryByteBuf buf, SubmitBlackoutSetupPayload payload) {
            buf.writeVarInt(payload.boardSize());
            buf.writeVarInt(payload.startTimerSeconds());
            buf.writeString(payload.boardTypeName());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
