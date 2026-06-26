package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AnnounceGoalFocusPayload(String goalId, boolean isReminder) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AnnounceGoalFocusPayload> ID = new CustomPacketPayload.Type<>(Constants.ANNOUNCE_GOAL_FOCUS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, AnnounceGoalFocusPayload> CODEC = new StreamCodec<>() {
        @Override
        public AnnounceGoalFocusPayload decode(RegistryFriendlyByteBuf buf) {
            String goalId = buf.readUtf();
            boolean isReminder = buf.readBoolean();
            return new AnnounceGoalFocusPayload(goalId, isReminder);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, AnnounceGoalFocusPayload payload) {
            buf.writeUtf(payload.goalId);
            buf.writeBoolean(payload.isReminder);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
