package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.EnterDimensionGoal;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import static me.marin.lockout.server.LockoutServer.lockout;

public class AfterPlayerChangeWorldEventHandler implements ServerEntityLevelChangeEvents.AfterPlayerChange {

    @Override
    public void afterChangeLevel(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof EnterDimensionGoal enterDimensionGoal)) continue;
            if (goal.isCompleted()) continue;

            if (destination.dimension() == enterDimensionGoal.getWorldRegistryKey()) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
