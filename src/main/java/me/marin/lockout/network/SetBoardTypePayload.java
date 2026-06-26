package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record SetBoardTypePayload(String boardType, List<String> excludedGoals) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetBoardTypePayload> ID = new CustomPacketPayload.Type<>(Constants.SET_BOARD_TYPE_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBoardTypePayload> CODEC = new StreamCodec<>() {
        @Override
        public SetBoardTypePayload decode(RegistryFriendlyByteBuf buf) {
            String boardType = buf.readUtf();
            int size = buf.readVarInt();
            List<String> excludedGoals = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                excludedGoals.add(buf.readUtf());
            }
            return new SetBoardTypePayload(boardType, excludedGoals);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SetBoardTypePayload payload) {
            buf.writeUtf(payload.boardType);
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
