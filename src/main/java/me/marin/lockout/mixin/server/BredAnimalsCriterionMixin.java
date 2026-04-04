package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.BreedAnimalGoal;
import me.marin.lockout.lockout.interfaces.BreedUniqueAnimalsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancement.criterion.BredAnimalsCriterion;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;

@Mixin(BredAnimalsCriterion.class)
public class BredAnimalsCriterionMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    public void onBreedAnimal(ServerPlayerEntity player, AnimalEntity parent, AnimalEntity partner, @Nullable PassiveEntity child, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof BreedAnimalGoal breedAnimalGoal) {
                if (parent.getType().equals(breedAnimalGoal.getAnimal())) {
                    lockout.completeGoal(breedAnimalGoal, player);
                }
            } else if (goal instanceof BreedUniqueAnimalsGoal breedUniqueAnimalsGoal) {
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUuid());
                lockout.bredAnimalTypes.computeIfAbsent(team, t -> new LinkedHashSet<>());
                boolean newAnimal = lockout.bredAnimalTypes.get(team).add(parent.getType());
                
                // Track per-player for statistics
                lockout.playerBredAnimals.computeIfAbsent(player.getUuid(), p -> new LinkedHashSet<>());
                lockout.playerBredAnimals.get(player.getUuid()).add(parent.getType());
                
                // Track first contributor
                if (newAnimal) {
                    lockout.firstBredAnimalContributor.putIfAbsent(team, new HashMap<>());
                    lockout.firstBredAnimalContributor.get(team).put(parent.getType(), player.getUuid());
                }
                
                int size = lockout.bredAnimalTypes.get(team).size();

                team.sendTooltipUpdate(breedUniqueAnimalsGoal);
                if (size >= breedUniqueAnimalsGoal.getAmount()) {
                    lockout.completeGoal(breedUniqueAnimalsGoal, team);
                }
            }

        }
    }

}
