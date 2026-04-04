package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record DownloadStatisticsPayload(String filename, String content) implements CustomPayload {
    public static final Id<DownloadStatisticsPayload> ID = new Id<>(Constants.DOWNLOAD_STATISTICS_PACKET);
    public static final PacketCodec<RegistryByteBuf, DownloadStatisticsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            DownloadStatisticsPayload::filename,
            PacketCodecs.STRING,
            DownloadStatisticsPayload::content,
            DownloadStatisticsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
