package me.marin.lockout.server.handlers;

import me.marin.lockout.LockoutInitializer;
import me.marin.lockout.network.LockoutVersionPayload;
import me.marin.lockout.network.SetBoardTypePayload;
import me.marin.lockout.network.UpdatePickBanSessionPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import me.marin.lockout.server.LockoutServer;
import me.marin.lockout.server.PickBanSession;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static me.marin.lockout.server.LockoutServer.waitingForVersionPacketPlayersMap;

public class PlayerJoinEventHandler implements ServerPlayConnectionEvents.Join {
    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender packetSender, MinecraftServer minecraftServer) {
        // Check if the client has the correct mod version:
        // 1. Send the Lockout version packet
        // 2. Follow it with a Minecraft ping packet (id is hash of 'username + version' string)
        // 3. Wait for the packets.
        // 4. If ping response is received before the version response, they don't have the mod
        // 5. Otherwise, compare the versions, and kick them if needed.

        ServerPlayerEntity player = handler.getPlayer();
        int id = (player.getName().getString() + LockoutInitializer.MOD_VERSION.getFriendlyString()).hashCode();

        // Send version packet first
        ServerPlayNetworking.send(player, new LockoutVersionPayload(LockoutInitializer.MOD_VERSION.getFriendlyString()));
        
        // Delay the ping by a few ticks to ensure the client processes the version payload first
        // This prevents the vanilla pong from arriving before the custom payload response
        new Thread(() -> {
            try {
                Thread.sleep(100); // 100ms delay (~2 ticks)
                minecraftServer.execute(() -> {
                    if (player.networkHandler != null && player.networkHandler.isConnectionOpen()) {
                        player.networkHandler.sendPacket(new CommonPingS2CPacket(id));
                        waitingForVersionPacketPlayersMap.put(player, id);
                    }
                });
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
        
        // Always sync server-side picks/bans to the joining player (even if empty, to clear client-side data)
        ServerPlayNetworking.send(player, new UpdatePicksBansPayload(
            LockoutServer.SERVER_PICKS,
            LockoutServer.SERVER_BANS,
            LockoutServer.SERVER_GOAL_TO_PLAYER_MAP
        ));
        
        // Sync board type if one is set
        if (LockoutServer.boardType != null && !LockoutServer.boardType.isEmpty()) {
            ServerPlayNetworking.send(player, new SetBoardTypePayload(LockoutServer.boardType, LockoutServer.boardTypeExcludedGoals));
        }
        
        // Sync locate data to the joining player
        if (!LockoutServer.BIOME_LOCATE_DATA.isEmpty() || !LockoutServer.STRUCTURE_LOCATE_DATA.isEmpty()) {
            ServerPlayNetworking.send(player, new me.marin.lockout.network.SyncLocateDataPayload(
                new java.util.HashMap<>(LockoutServer.BIOME_LOCATE_DATA),
                new java.util.HashMap<>(LockoutServer.STRUCTURE_LOCATE_DATA)
            ));
        }
        
        // If there's an active pick/ban session, sync it to the joining player
        if (LockoutServer.activePickBanSession != null) {
            PickBanSession session = LockoutServer.activePickBanSession;
            
            // Use goal-to-player map from the session
            java.util.Map<String, String> goalToPlayerMap = session.getGoalToPlayerMap();
            
            // Send current session state to the player
            UpdatePickBanSessionPayload payload = new UpdatePickBanSessionPayload(
                session.getCurrentRound(),
                session.isTeam1Turn(),
                session.getTeam1Name(),
                session.getTeam2Name(),
                session.getAllLockedPicks(),
                session.getAllLockedBans(),
                session.getPendingPicks(),
                session.getPendingBans(),
                session.getSelectionLimit(),
                goalToPlayerMap,
                session.getMaxRounds()
            );
            ServerPlayNetworking.send(player, payload);
            
            // Notify the player about the ongoing session
            player.sendMessage(
                Text.literal("A pick/ban session is in progress. Round " + session.getCurrentRound() + "/" + session.getMaxRounds()).withColor(0xFFAA00),
                false
            );
        }
    }
}

