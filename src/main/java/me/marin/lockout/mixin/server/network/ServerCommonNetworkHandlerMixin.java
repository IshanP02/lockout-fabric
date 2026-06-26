package me.marin.lockout.mixin.server.network;

import com.mojang.authlib.GameProfile;
import me.marin.lockout.LockoutInitializer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonNetworkHandlerMixin {

    @Shadow protected abstract GameProfile playerProfile();

    @Shadow @Final protected MinecraftServer server;

    @Inject(method = "handlePong", at = @At("TAIL"))
    public void onPong(ServerboundPongPacket packet, CallbackInfo ci) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerProfile().id());
            int id = packet.getId();

            // Avoid disconnecting integrated singleplayer clients due to packet ordering races.
            if (!server.isDedicatedServer()) return;

            if (LockoutServer.waitingForVersionPacketPlayersMap.containsKey(player)) {
                if (LockoutServer.waitingForVersionPacketPlayersMap.get(player) == id) {
                    player.connection.disconnect(Component.literal("Missing Lockout mod.\nServer is using Lockout v" + LockoutInitializer.MOD_VERSION.getFriendlyString() + "."));
                }
            }
        });
    }

}
