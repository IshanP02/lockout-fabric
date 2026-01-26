package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record SetBoardTypePayload(String boardType, List<String> excludedGoals) implements CustomPayload {
    public static final Id<SetBoardTypePayload> ID = new Id<>(Constants.SET_BOARD_TYPE_PACKET);
    public static final PacketCodec<RegistryByteBuf, SetBoardTypePayload> CODEC = new PacketCodec<>() {
        @Override
        public SetBoardTypePayload decode(RegistryByteBuf buf) {
            String boardType = buf.readString();
            int size = buf.readVarInt();
            List<String> excludedGoals = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                excludedGoals.add(buf.readString());
            }
            return new SetBoardTypePayload(boardType, excludedGoals);
        }

        @Override
        public void encode(RegistryByteBuf buf, SetBoardTypePayload payload) {
            buf.writeString(payload.boardType);
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
