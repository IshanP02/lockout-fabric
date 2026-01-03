package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record StartPickBanSessionPayload(String team1Name, String team2Name, int selectionLimit) implements CustomPayload {
    public static final Id<StartPickBanSessionPayload> ID = new Id<>(Constants.START_PICK_BAN_SESSION_PACKET);
    public static final PacketCodec<RegistryByteBuf, StartPickBanSessionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            StartPickBanSessionPayload::team1Name,
            PacketCodecs.STRING,
            StartPickBanSessionPayload::team2Name,
            PacketCodecs.INTEGER,
            StartPickBanSessionPayload::selectionLimit,
            StartPickBanSessionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
