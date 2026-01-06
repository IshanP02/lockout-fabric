package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record AnnounceGoalFocusPayload(String goalId, boolean isReminder) implements CustomPayload {
    public static final Id<AnnounceGoalFocusPayload> ID = new Id<>(Constants.ANNOUNCE_GOAL_FOCUS_PACKET);
    public static final PacketCodec<RegistryByteBuf, AnnounceGoalFocusPayload> CODEC = new PacketCodec<>() {
        @Override
        public AnnounceGoalFocusPayload decode(RegistryByteBuf buf) {
            String goalId = buf.readString();
            boolean isReminder = buf.readBoolean();
            return new AnnounceGoalFocusPayload(goalId, isReminder);
        }

        @Override
        public void encode(RegistryByteBuf buf, AnnounceGoalFocusPayload payload) {
            buf.writeString(payload.goalId);
            buf.writeBoolean(payload.isReminder);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
