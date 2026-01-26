package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record UploadBoardTypePayload(String boardTypeName, List<String> excludedGoals) implements CustomPayload {
    public static final Id<UploadBoardTypePayload> ID = new Id<>(Constants.UPLOAD_BOARD_TYPE_PACKET);
    public static final PacketCodec<RegistryByteBuf, UploadBoardTypePayload> CODEC = new PacketCodec<>() {
        @Override
        public UploadBoardTypePayload decode(RegistryByteBuf buf) {
            String boardTypeName = buf.readString();
            int size = buf.readVarInt();
            List<String> excludedGoals = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                excludedGoals.add(buf.readString());
            }
            return new UploadBoardTypePayload(boardTypeName, excludedGoals);
        }

        @Override
        public void encode(RegistryByteBuf buf, UploadBoardTypePayload payload) {
            buf.writeString(payload.boardTypeName);
            buf.writeVarInt(payload.excludedGoals.size());
            for (String goalId : payload.excludedGoals) {
                buf.writeString(goalId);
            }
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
