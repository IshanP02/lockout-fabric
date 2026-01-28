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
import java.util.UUID;

import static me.marin.lockout.server.LockoutServer.waitingForVersionPacketPlayersMap;

public class PlayerJoinEventHandler implements ServerPlayConnectionEvents.Join {
    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender packetSender, MinecraftServer minecraftServer) {
        // Check if the client has the correct mod version:
        // 1. Send the Lockout version packet
        // 2. Store timestamp in waiting map
        // 3. If version response arrives within timeout, validate version
        // 4. If timeout expires, kick player for missing mod

        ServerPlayerEntity player = handler.getPlayer();

        // Send version packet first
        ServerPlayNetworking.send(player, new LockoutVersionPayload(LockoutInitializer.MOD_VERSION.getFriendlyString()));
        
        waitingForVersionPacketPlayersMap.put(player.getUuid(), System.currentTimeMillis());
        
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

