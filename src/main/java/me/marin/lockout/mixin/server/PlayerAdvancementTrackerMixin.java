package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.have_more.HaveMostAdvancementsGoal;
import me.marin.lockout.lockout.goals.advancement.GetHotTouristDestinationsAdvancementGoal;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.interfaces.GetUniqueAdvancementsGoal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.interfaces.VisitAllSpecificBiomesGoal;
import me.marin.lockout.lockout.interfaces.VisitBiomeGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Objects;

import me.marin.lockout.lockout.interfaces.VisitUniqueBiomesGoal;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayer player;

    @Redirect(method = "lambda$award$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V") )
    public void onBroadcastInChat(PlayerList instance, Component message, boolean overlay) {
        Lockout lockout = LockoutServer.lockout;

        // Prevent spectator advancements from showing in chat
        if (!Lockout.isLockoutRunning(lockout) || lockout.isLockoutPlayer(player.getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }

    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/Advancement;rewards()Lnet/minecraft/advancements/AdvancementRewards;") )
    public void onGrantCriterion(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player.getUUID())) return;
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            // Track player advancements for HaveMostAdvancementsGoal regardless of goal completion
            if (goal instanceof HaveMostAdvancementsGoal) {
                Optional<DisplayInfo> advancementDisplay = advancement.value().display();
                if (advancementDisplay.isPresent() && advancementDisplay.get().shouldAnnounceChat()) {
                    // Increment advancement count for this player
                    lockout.playerAdvancements.putIfAbsent(player.getUUID(), 0);
                    lockout.playerAdvancements.merge(player.getUUID(), 1, Integer::sum);

                    int playerAdvancements = lockout.playerAdvancements.get(player.getUUID());

                    // Send client-side feedback to the player
                    Holder<net.minecraft.sounds.SoundEvent> soundEntry = SoundEvents.NOTE_BLOCK_CHIME;
                    player.connection.send(
                        new ClientboundSoundPacket(
                            soundEntry,
                            SoundSource.MASTER,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            2f,
                            0.5f,
                            player.level().getRandom().nextLong()
                        )
                    );
                    if (playerAdvancements % 5 == 0) {
                        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "" + ChatFormatting.ITALIC + "You have completed " + playerAdvancements + " advancements."));
                    }
                    player.sendSystemMessage(Component.literal("Advancements: " + playerAdvancements));

                    // If this player now has more advancements than current leader, update completion
                    if (playerAdvancements > lockout.mostAdvancements) {
                        if (!Objects.equals(lockout.mostAdvancementsPlayer, player.getUUID())) {
                            lockout.updateGoalCompletion(goal, player.getUUID());
                        }
                        lockout.mostAdvancementsPlayer = player.getUUID();
                        lockout.mostAdvancements = playerAdvancements;
                    }
                    // Send tooltip updates to all teams whenever anyone makes progress
                    for (LockoutTeam teamToUpdate : lockout.getTeams()) {
                        ((LockoutTeamServer) teamToUpdate).sendTooltipUpdate((HaveMostAdvancementsGoal) goal, true);
                    }
                }
            }

            if (goal.isCompleted()) continue;

            if (goal instanceof AdvancementGoal advancementGoal) {
                if (advancementGoal.getAdvancements().contains(advancement.id())) {
                    lockout.completeGoal(goal, player);
                }
            }
            if (goal instanceof GetUniqueAdvancementsGoal getUniqueAdvancementsGoal) {
                Optional<DisplayInfo> advancementDisplay = advancement.value().display();
                if (advancementDisplay.isPresent()) {
                    getUniqueAdvancementsGoal.getTrackerMap().putIfAbsent(team, new LinkedHashSet<>());
                    boolean newAdvancement = getUniqueAdvancementsGoal.getTrackerMap().get(team).add(advancement.id());
                    
                    // Track per-player unique advancements for statistics
                    lockout.playerUniqueAdvancements.putIfAbsent(player.getUUID(), new LinkedHashSet<>());
                    lockout.playerUniqueAdvancements.get(player.getUUID()).add(advancement.id());
                    
                    // Track first contributor
                    if (newAdvancement) {
                        lockout.firstAdvancementContributor.putIfAbsent(team, new HashMap<>());
                        lockout.firstAdvancementContributor.get(team).put(advancement.id(), player.getUUID());
                    }
                    
                    // Also increment global counter for HaveMostAdvancementsGoal compatibility
                    lockout.playerAdvancements.putIfAbsent(player.getUUID(), 0);
                    lockout.playerAdvancements.merge(player.getUUID(), 1, Integer::sum);

                    int size = getUniqueAdvancementsGoal.getTrackerMap().get(team).size();

                    team.sendTooltipUpdate(getUniqueAdvancementsGoal);
                    if (size >= getUniqueAdvancementsGoal.getAmount()) {
                        lockout.completeGoal(goal, team);
                    }
                }
            }
        }
    }

    private static final Identifier ADVENTURING_TIME = Identifier.fromNamespaceAndPath("minecraft", "adventure/adventuring_time");
    private static final Identifier HOT_TOURIST_DESTINATIONS = Identifier.fromNamespaceAndPath("minecraft", "nether/explore_nether");
    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;isDone()Z", ordinal = 1, shift = At.Shift.BEFORE) )
    public void onAdvancementProgress(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player.getUUID())) return;

        if (!advancement.id().equals(ADVENTURING_TIME) && !advancement.id().equals(HOT_TOURIST_DESTINATIONS)) return;
        Identifier biomeId = Identifier.parse(criterionName);
        LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());

        // For nether biomes (HOT_TOURIST_DESTINATIONS), ensure the biome is tracked before processing goals
        // Track whether this was a new biome so we can use it later for tooltip updates
        boolean wasNewBiomeForNether = false;
        if (advancement.id().equals(HOT_TOURIST_DESTINATIONS)) {
            lockout.biomesVisited.putIfAbsent(team, new LinkedHashSet<>());
            wasNewBiomeForNether = lockout.biomesVisited.get(team).add(biomeId);
            
            // Track per-player for statistics
            lockout.playerBiomesVisited.computeIfAbsent(player.getUUID(), p -> new LinkedHashSet<>());
            lockout.playerBiomesVisited.get(player.getUUID()).add(biomeId);
            
            // Track first contributor for this biome
            if (wasNewBiomeForNether) {
                lockout.firstBiomeContributor.putIfAbsent(team, new HashMap<>());
                lockout.firstBiomeContributor.get(team).put(biomeId, player.getUUID());
            }
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof VisitBiomeGoal visitBiomeGoal) {
                if (visitBiomeGoal.getBiomes().contains(biomeId)) {
                    lockout.completeGoal(goal, player);
                }
            }

            if (goal instanceof VisitAllSpecificBiomesGoal visitAllSpecificBiomesGoal) {
                if (visitAllSpecificBiomesGoal.getBiomes().contains(biomeId)) {
                    visitAllSpecificBiomesGoal.getTrackerMap().computeIfAbsent(team, t -> new LinkedHashSet<>());
                    visitAllSpecificBiomesGoal.getTrackerMap().get(team).add(biomeId);

                    int size = visitAllSpecificBiomesGoal.getTrackerMap().get(team).size();

                    ((LockoutTeamServer) team).sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                    if (size >= visitAllSpecificBiomesGoal.getBiomes().size()) {
                        lockout.completeGoal(visitAllSpecificBiomesGoal, team);
                    }
                }
            }

            if (goal instanceof VisitUniqueBiomesGoal visitUniqueBiomesGoal) {
                // Track biomes from both ADVENTURING_TIME and HOT_TOURIST_DESTINATIONS
                visitUniqueBiomesGoal.getTrackerMap().putIfAbsent(team, new LinkedHashSet<>());
                var set = visitUniqueBiomesGoal.getTrackerMap().get(team);
                
                // If this is a nether biome, we already added it above, use that result
                // Otherwise, add it now for overworld biomes
                boolean added = advancement.id().equals(HOT_TOURIST_DESTINATIONS) 
                    ? wasNewBiomeForNether 
                    : set.add(biomeId);
                
                // Track per-player for statistics (overworld biomes)
                if (advancement.id().equals(ADVENTURING_TIME)) {
                    lockout.playerBiomesVisited.computeIfAbsent(player.getUUID(), p -> new LinkedHashSet<>());
                    lockout.playerBiomesVisited.get(player.getUUID()).add(biomeId);
                    
                    // Track first contributor for overworld biomes
                    if (added) {
                        lockout.firstBiomeContributor.putIfAbsent(team, new HashMap<>());
                        lockout.firstBiomeContributor.get(team).putIfAbsent(biomeId, player.getUUID());
                    }
                }

                int size = set.size();

                if (added) {
                    // send updates for every VisitUniqueBiomesGoal on the board
                    for (Goal g : lockout.getBoard().getGoals()) {
                        if (g instanceof VisitUniqueBiomesGoal uniqueBiome) {
                            VisitUniqueBiomesGoal visitGoal = (VisitUniqueBiomesGoal)g;
                            int amount = visitGoal.getAmount();
                            if(size <= amount) {
                                team.sendTooltipUpdate(uniqueBiome);
                            }
                        }
                    }
                }

                if (size >= visitUniqueBiomesGoal.getAmount()) {
                    lockout.completeGoal(goal, team);
                }
            }
            
            // Update GetHotTouristDestinationsAdvancementGoal tooltip for nether biomes
            if (goal instanceof GetHotTouristDestinationsAdvancementGoal hotTouristGoal && advancement.id().equals(HOT_TOURIST_DESTINATIONS)) {
                team.sendTooltipUpdate(hotTouristGoal, true);
            }

        }
    }
}