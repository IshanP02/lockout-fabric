package me.marin.lockout.network;

import me.marin.lockout.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DownloadStatisticsPayload(String filename, String content) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DownloadStatisticsPayload> ID = new CustomPacketPayload.Type<>(Constants.DOWNLOAD_STATISTICS_PACKET);
    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadStatisticsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DownloadStatisticsPayload::filename,
            ByteBufCodecs.STRING_UTF8,
            DownloadStatisticsPayload::content,
            DownloadStatisticsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
