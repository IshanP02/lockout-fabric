package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record OpenBlackoutSetupPayload(int boardSize, int startTimerSeconds, String selectedBoardType, List<String> availableBoardTypes) implements CustomPayload {
    public static final Id<OpenBlackoutSetupPayload> ID = new Id<>(Constants.OPEN_BLACKOUT_SETUP_PACKET);
    public static final PacketCodec<RegistryByteBuf, OpenBlackoutSetupPayload> CODEC = new PacketCodec<>() {
        @Override
        public OpenBlackoutSetupPayload decode(RegistryByteBuf buf) {
            int boardSize = buf.readVarInt();
            int startTimerSeconds = buf.readVarInt();
            String selectedBoardType = buf.readString();
            int listSize = buf.readVarInt();
            List<String> availableBoardTypes = new ArrayList<>(listSize);
            for (int i = 0; i < listSize; i++) {
                availableBoardTypes.add(buf.readString());
            }
            return new OpenBlackoutSetupPayload(boardSize, startTimerSeconds, selectedBoardType, availableBoardTypes);
        }

        @Override
        public void encode(RegistryByteBuf buf, OpenBlackoutSetupPayload payload) {
            buf.writeVarInt(payload.boardSize());
            buf.writeVarInt(payload.startTimerSeconds());
            buf.writeString(payload.selectedBoardType());
            buf.writeVarInt(payload.availableBoardTypes().size());
            for (String boardType : payload.availableBoardTypes()) {
                buf.writeString(boardType);
            }
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
