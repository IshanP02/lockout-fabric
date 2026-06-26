package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;

import static me.marin.lockout.server.LockoutServer.compassHandler;
import static me.marin.lockout.server.LockoutServer.lockout;

public class AfterRespawnEventHandler implements ServerPlayerEvents.AfterRespawn {

    @Override
    public void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (lockout.isSoloBlackout()) return;
        if (!lockout.isLockoutPlayer(newPlayer.getUUID())) return;
        if (alive) return; // end exit portal

        int slot = compassHandler.compassSlots.getOrDefault(newPlayer.getUUID(), 0);
        if (slot == 40) {
            newPlayer.getInventory().setItem(40, compassHandler.newCompass());
        }
        if (slot >= 0 && slot <= 35) {
            newPlayer.getInventory().setItem(slot, compassHandler.newCompass());
        }
                // Re-apply waypoint color after respawn
        LockoutTeam playerTeam = lockout.getPlayerTeam(newPlayer.getUUID());
        if (playerTeam != null) {
            LockoutServer.updatePlayerWaypointColor(newPlayer, playerTeam.getColor());
        }
    }
}
