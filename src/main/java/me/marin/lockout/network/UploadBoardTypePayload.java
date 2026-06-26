package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record UploadBoardTypePayload(String boardTypeName, List<String> excludedGoals) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UploadBoardTypePayload> ID = new CustomPacketPayload.Type<>(Constants.UPLOAD_BOARD_TYPE_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, UploadBoardTypePayload> CODEC = new StreamCodec<>() {
        @Override
        public UploadBoardTypePayload decode(RegistryFriendlyByteBuf buf) {
            String boardTypeName = buf.readUtf();
            int size = buf.readVarInt();
            List<String> excludedGoals = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                excludedGoals.add(buf.readUtf());
            }
            return new UploadBoardTypePayload(boardTypeName, excludedGoals);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UploadBoardTypePayload payload) {
            buf.writeUtf(payload.boardTypeName);
            buf.writeVarInt(payload.excludedGoals.size());
            for (String goalId : payload.excludedGoals) {
                buf.writeUtf(goalId);
            }
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
