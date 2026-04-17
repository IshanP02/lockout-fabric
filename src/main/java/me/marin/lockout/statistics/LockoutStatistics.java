package me.marin.lockout.statistics;

import lombok.Getter;
import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.*;
import me.marin.lockout.lockout.goals.have_more.HaveMostAdvancementsGoal;
import me.marin.lockout.lockout.goals.have_more.HaveMostCreeperKillsGoal;
import me.marin.lockout.lockout.goals.have_more.HaveMostUniqueCraftsGoal;
import me.marin.lockout.lockout.goals.have_more.HaveMostUniqueSmeltsGoal;
import me.marin.lockout.lockout.goals.have_more.HaveMostXPLevelsGoal;
import me.marin.lockout.lockout.goals.opponent.*;
import me.marin.lockout.lockout.goals.wear_armor.WearCarvedPumpkinFor5MinutesGoal;
import me.marin.lockout.lockout.goals.misc.*;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LockoutStatistics {

    @Getter
    private final Lockout lockout;
    private List<LockoutTeam> winners = new ArrayList<>();
    
    // Player-level statistics
    private final Map<UUID, Integer> playerDeaths = new HashMap<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    private final Map<UUID, Map<Goal, Double>> playerGoalContributions = new HashMap<>();
    
    public LockoutStatistics(Lockout lockout) {
        this.lockout = lockout;
        initializePlayerMaps();
    }
    
    private void initializePlayerMaps() {
        for (LockoutTeam team : lockout.getTeams()) {
            if (team instanceof LockoutTeamServer serverTeam) {
                for (UUID playerId : serverTeam.getPlayers()) {
                    playerDeaths.put(playerId, 0);
                    playerKills.put(playerId, 0);
                    playerGoalContributions.put(playerId, new HashMap<>());
                }
            }
        }
    }
    
    /**
     * Record a player death (excluding task-required deaths)
     */
    public void recordPlayerDeath(UUID playerId, boolean isTaskDeath) {
        if (!isTaskDeath) {
            playerDeaths.merge(playerId, 1, Integer::sum);
        }
    }
    
    /**
     * Record a player kill
     */
    public void recordPlayerKill(UUID killerId) {
        playerKills.merge(killerId, 1, Integer::sum);
    }
    
    /**
     * Calculate goal contribution for all players and completed goals
     */
    public void calculateGoalContributions() {
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null || !goal.isCompleted()) continue;
            
            LockoutTeam completedTeam = goal.getCompletedTeam();
            if (completedTeam == null || !(completedTeam instanceof LockoutTeamServer serverTeam)) continue;
            
            // Skip if contributions for this goal have already been calculated (snapshot at completion)
            boolean alreadyCalculated = serverTeam.getPlayers().stream()
                .allMatch(playerId -> playerGoalContributions.get(playerId).containsKey(goal));
            
            if (alreadyCalculated) continue;
            
            // Calculate contribution for this goal
            Map<UUID, Double> contributions = calculateGoalContribution(goal, serverTeam);
            
            // Store contributions
            for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
                playerGoalContributions.get(entry.getKey()).put(goal, entry.getValue());
            }
        }
    }
    
    /**
     * Capture contribution snapshot for a specific goal at the moment of completion
     * This ensures contributions don't change if tracking continues after goal completion
     */
    public void captureGoalContribution(Goal goal, LockoutTeamServer team) {
        if (team == null) return;
        
        // Calculate contribution for this goal based on current tracking data
        Map<UUID, Double> contributions = calculateGoalContribution(goal, team);
        
        // Store contributions as a snapshot
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            playerGoalContributions.get(entry.getKey()).put(goal, entry.getValue());
        }
    }
    
    /**
     * Clear contributions for a specific goal (used when "Have More" goals change ownership)
     * Sets all players' contributions for this goal back to 0.0
     */
    public void clearGoalContribution(Goal goal) {
        for (Map<Goal, Double> contributions : playerGoalContributions.values()) {
            contributions.remove(goal);
        }
    }
    
    /**
     * Calculate individual player contributions for a specific goal
     * Returns a map of player UUID to contribution value (0.0 to 1.0)
     */
    private Map<UUID, Double> calculateGoalContribution(Goal goal, LockoutTeamServer team) {
        Map<UUID, Double> contributions = new HashMap<>();
        List<UUID> teamPlayers = team.getPlayers();
        
        // Initialize all team members to 0 contribution
        for (UUID playerId : teamPlayers) {
            contributions.put(playerId, 0.0);
        }
        
        // Check if goal is trackable (team-based goal with progress data)
        if (goal instanceof Trackable<?, ?> trackable) {
            contributions = calculateTrackableGoalContribution(goal, trackable, team, teamPlayers);
        } else {
            // For non-trackable goals, check if there's player-specific tracking available
            Map<UUID, Double> playerSpecificContributions = calculatePlayerSpecificContribution(goal, team, teamPlayers);
            
            if (playerSpecificContributions != null) {
                contributions = playerSpecificContributions;
            } else {
                // Non-trackable goals: credit goes to the player who completed it
                String completedMessage = goal.getCompletedMessage();
                if (completedMessage != null && !completedMessage.isEmpty()) {
                    // Find the player who completed this goal
                    for (UUID playerId : teamPlayers) {
                        if (team.getPlayerName(playerId).equals(completedMessage)) {
                            contributions.put(playerId, 1.0);
                            break;
                        }
                    }
                } else {
                    // If no single completer, split equally among team
                    double equalShare = 1.0 / teamPlayers.size();
                    for (UUID playerId : teamPlayers) {
                        contributions.put(playerId, equalShare);
                    }
                }
            }
        }
        
        // Round to 2 decimal places
        for (UUID playerId : contributions.keySet()) {
            double value = contributions.get(playerId);
            contributions.put(playerId, Math.round(value * 100.0) / 100.0);
        }
        
        return contributions;
    }
    
    /**
     * Calculate contribution for trackable goals (like EatUniqueFoods, etc.)
     */
    private Map<UUID, Double> calculateTrackableGoalContribution(Goal goal, Trackable<?, ?> trackable, 
                                                                  LockoutTeamServer team, List<UUID> teamPlayers) {
        // First try to get player-specific contributions based on goal type
        Map<UUID, Double> playerContributions = calculatePlayerSpecificContribution(goal, team, teamPlayers);
        
        if (playerContributions != null) {
            return playerContributions;
        }
        
        // Fallback: split equally
        Map<UUID, Double> contributions = new HashMap<>();
        double equalShare = 1.0 / teamPlayers.size();
        for (UUID playerId : teamPlayers) {
            contributions.put(playerId, equalShare);
        }
        
        return contributions;
    }
    
    /**
     * Calculate contribution for goals that have player-specific tracking
     */
    private Map<UUID, Double> calculatePlayerSpecificContribution(Goal goal, LockoutTeamServer team, List<UUID> teamPlayers) {
        Map<UUID, Double> contributions = new HashMap<>();
        
        // Initialize all to 0
        for (UUID playerId : teamPlayers) {
            contributions.put(playerId, 0.0);
        }
        
        // Check for goals with individual player tracking
        
        // "Have More" goals - only the leader gets 1.0 contribution, everyone else gets 0.0
        if (goal instanceof HaveMostXPLevelsGoal) {
            UUID leader = lockout.mostLevelsPlayer;
            if (leader != null && teamPlayers.contains(leader)) {
                contributions.put(leader, 1.0);
            }
            return contributions;
        }
        
        if (goal instanceof HaveMostCreeperKillsGoal) {
            UUID leader = lockout.mostCreeperKillsPlayer;
            if (leader != null && teamPlayers.contains(leader)) {
                contributions.put(leader, 1.0);
            }
            return contributions;
        }
        
        if (goal instanceof HaveMostAdvancementsGoal) {
            UUID leader = lockout.mostAdvancementsPlayer;
            if (leader != null && teamPlayers.contains(leader)) {
                contributions.put(leader, 1.0);
            }
            return contributions;
        }
        
        if (goal instanceof HaveMostUniqueCraftsGoal) {
            UUID leader = lockout.mostUniqueCraftsPlayer;
            if (leader != null && teamPlayers.contains(leader)) {
                contributions.put(leader, 1.0);
            }
            return contributions;
        }
        
        if (goal instanceof HaveMostUniqueSmeltsGoal) {
            UUID leader = lockout.mostUniqueSmeltsPlayer;
            if (leader != null && teamPlayers.contains(leader)) {
                contributions.put(leader, 1.0);
            }
            return contributions;
        }
        
        // 1. Advancement-based goals - use playerUniqueAdvancements with first contributor tracking
        if (goal instanceof GetUniqueAdvancementsGoal) {
            Map<Identifier, UUID> firstContributor = lockout.firstAdvancementContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerUniqueAdvancements, firstContributor);
        }
        
        // 2. Food-based goals - use playerFoodsEaten size
        if (goal instanceof EatUniqueFoodsGoal) {
            Map<Item, UUID> firstContributor = lockout.firstFoodContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerFoodsEaten, firstContributor);
        }
        
        // 3. Biome visiting goals
        if (goal instanceof VisitUniqueBiomesGoal || goal instanceof VisitAllSpecificBiomesGoal) {
            Map<Identifier, UUID> firstContributor = lockout.firstBiomeContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerBiomesVisited, firstContributor);
        }
        
        // 4. Breeding animals goals
        if (goal instanceof BreedUniqueAnimalsGoal) {
            Map<EntityType<?>, UUID> firstContributor = lockout.firstBredAnimalContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerBredAnimals, firstContributor);
        }
        
        // 5. Looking at mobs goals
        if (goal instanceof LookAtUniqueMobsGoal) {
            Map<EntityType<?>, UUID> firstContributor = lockout.firstLookedAtMobContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerLookedAtMobs, firstContributor);
        }
        
        // 6. Killing hostile mobs goals
        if (goal instanceof KillUniqueHostileMobsGoal) {
            Map<EntityType<?>, UUID> firstContributor = lockout.firstHostileKillContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerKilledHostileMobs, firstContributor);
        }
        
        // 7. Killing arthropods goal
        if (goal.getGoalName().contains("Arthropod")) {
            return calculateProportionalContributionFromInt(teamPlayers, lockout.playerKilledArthropods);
        }
        
        // 8. Killing undead mobs goal
        if (goal.getGoalName().contains("Undead Mobs")) {
            return calculateProportionalContributionFromInt(teamPlayers, lockout.playerKilledUndeadMobs);
        }
        
        // 9. Kill 100 Mobs goal
        if (goal.getGoalName().equals("Kill 100 Mobs")) {
            return calculateProportionalContributionFromInt(teamPlayers, lockout.playerMobsKilled);
        }
        
        // 10. Pumpkin wearing goal (5 minutes = 6000 ticks)
        if (goal instanceof WearCarvedPumpkinFor5MinutesGoal) {
            return calculateProportionalContributionFromTime(teamPlayers, lockout.pumpkinWearTime, 5 * 60 * 20L);
        }
        
        // 11. Effects applied goal  
        if (goal instanceof HaveEffectsAppliedForXMinutesGoal effectsGoal) {
            long requiredTicks = effectsGoal.getMinutes() * 60 * 20L;
            return calculateProportionalContributionFromTime(teamPlayers, lockout.appliedEffectsTime, requiredTicks);
        }
        
        // 12. Damage taken goal
        if (goal instanceof Take200DamageGoal) {
            return calculateProportionalContributionFromDouble(teamPlayers, lockout.playerDamageTaken);
        }
        
        // 13. Damage dealt goal
        if (goal instanceof Deal400DamageGoal) {
            return calculateProportionalContributionFromDouble(teamPlayers, lockout.playerDamageDealt);
        }
        
        // 14. Damage types taken goal
        if (goal instanceof DamagedByUniqueSourcesGoal) {
            Map<RegistryKey<DamageType>, UUID> firstContributor = lockout.firstDamageTypeContributor.get(team);
            return calculateProportionalContribution(teamPlayers, lockout.playerDamageTypesTaken, firstContributor);
        }
        
        // 15. Opponent goals - each player on winning team gets 0.5
        if (isOpponentGoalWithEqualCredit(goal)) {
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, 0.5);
            }
            return contributions;
        }
        
        // 16. For other goals, check completedMessage for single contributor
        String completedMessage = goal.getCompletedMessage();
        if (completedMessage != null && !completedMessage.isEmpty()) {
            boolean foundPlayer = false;
            for (UUID playerId : teamPlayers) {
                String playerName = team.getPlayerName(playerId);
                if (playerName != null && playerName.equals(completedMessage)) {
                    contributions.put(playerId, 1.0);
                    foundPlayer = true;
                } else {
                    contributions.put(playerId, 0.0);
                }
            }
            if (foundPlayer) {
                return contributions;
            }
        }
        
        // No specific tracking found
        return null;
    }
    
    /**
     * Helper method to calculate proportional contribution from a map of Set<T>
     * Gives full credit to the first player who contributed each item (no shared credit)
     */
    private <T> Map<UUID, Double> calculateProportionalContribution(
            List<UUID> teamPlayers, 
            Map<UUID, Set<T>> playerData,
            Map<T, UUID> firstContributorMap) {
        Map<UUID, Double> contributions = new HashMap<>();
        
        // Initialize all players to 0.0 contribution
        for (UUID playerId : teamPlayers) {
            contributions.put(playerId, 0.0);
        }
        
        // Calculate the union of all player sets to get total unique items
        Set<T> union = new HashSet<>();
        for (UUID playerId : teamPlayers) {
            union.addAll(playerData.getOrDefault(playerId, new HashSet<>()));
        }
        
        if (union.isEmpty()) {
            // No data, split equally
            double equalShare = 1.0 / teamPlayers.size();
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, equalShare);
            }
            return contributions;
        }
        
        // For each unique item, give full credit to the first contributor
        for (T item : union) {
            UUID firstContributor = firstContributorMap != null ? firstContributorMap.get(item) : null;
            
            if (firstContributor != null && teamPlayers.contains(firstContributor)) {
                // Give full credit to the first contributor
                contributions.put(firstContributor, contributions.get(firstContributor) + 1.0);
            } else {
                // Fallback: if we don't have first contributor data, split credit among players who have it
                List<UUID> playersWithItem = new ArrayList<>();
                for (UUID playerId : teamPlayers) {
                    Set<T> playerSet = playerData.getOrDefault(playerId, new HashSet<>());
                    if (playerSet.contains(item)) {
                        playersWithItem.add(playerId);
                    }
                }
                
                if (!playersWithItem.isEmpty()) {
                    double creditPerPlayer = 1.0 / playersWithItem.size();
                    for (UUID playerId : playersWithItem) {
                        contributions.put(playerId, contributions.get(playerId) + creditPerPlayer);
                    }
                }
            }
        }
        
        // Normalize by total unique items to get proportional contribution (0.0 to 1.0)
        int totalUnique = union.size();
        for (UUID playerId : teamPlayers) {
            contributions.put(playerId, contributions.get(playerId) / totalUnique);
        }
        
        return contributions;
    }
    
    /**
     * Helper method to calculate proportional contribution from a map of Integer
     */
    private Map<UUID, Double> calculateProportionalContributionFromInt(List<UUID> teamPlayers, Map<UUID, Integer> playerData) {
        Map<UUID, Double> contributions = new HashMap<>();
        int total = 0;
        
        for (UUID playerId : teamPlayers) {
            total += playerData.getOrDefault(playerId, 0);
        }
        
        if (total > 0) {
            for (UUID playerId : teamPlayers) {
                double contribution = (double) playerData.getOrDefault(playerId, 0) / total;
                contributions.put(playerId, contribution);
            }
        } else {
            // No data, split equally
            double equalShare = 1.0 / teamPlayers.size();
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, equalShare);
            }
        }
        
        return contributions;
    }
    
    /**
     * Helper method to calculate proportional contribution from a map of Long
     */
    /**
     * Helper method to calculate proportional contribution from a map of Long (time-based goals)
     * Each player's contribution is their proportion of the total team time
     * The goal completes when total team time reaches requirement, so contributions always sum to 1.0
     */
    private Map<UUID, Double> calculateProportionalContributionFromTime(List<UUID> teamPlayers, Map<UUID, Long> playerData, long requiredTicks) {
        Map<UUID, Double> contributions = new HashMap<>();
        long totalTime = 0;
        
        // Calculate total time contributed by all team members
        for (UUID playerId : teamPlayers) {
            totalTime += playerData.getOrDefault(playerId, 0L);
        }
        
        if (totalTime > 0) {
            // Each player gets credit proportional to their contribution of the total
            for (UUID playerId : teamPlayers) {
                long playerTime = playerData.getOrDefault(playerId, 0L);
                double contribution = (double) playerTime / totalTime;
                contributions.put(playerId, contribution);
            }
        } else {
            // If no one contributed, split equally
            double equalShare = 1.0 / teamPlayers.size();
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, equalShare);
            }
        }
        
        return contributions;
    }
    
    /**
     * Helper method to calculate proportional contribution from a map of Long (non-time-based)
     */
    private Map<UUID, Double> calculateProportionalContributionFromLong(List<UUID> teamPlayers, Map<UUID, Long> playerData) {
        Map<UUID, Double> contributions = new HashMap<>();
        long total = 0;
        
        for (UUID playerId : teamPlayers) {
            total += playerData.getOrDefault(playerId, 0L);
        }
        
        if (total > 0) {
            for (UUID playerId : teamPlayers) {
                double contribution = (double) playerData.getOrDefault(playerId, 0L) / total;
                contributions.put(playerId, contribution);
            }
        } else {
            // No data, split equally
            double equalShare = 1.0 / teamPlayers.size();
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, equalShare);
            }
        }
        
        return contributions;
    }
    
    /**
     * Helper method to calculate proportional contribution from a map of Double
     */
    private Map<UUID, Double> calculateProportionalContributionFromDouble(List<UUID> teamPlayers, Map<UUID, Double> playerData) {
        Map<UUID, Double> contributions = new HashMap<>();
        double total = 0.0;
        
        for (UUID playerId : teamPlayers) {
            total += playerData.getOrDefault(playerId, 0.0);
        }
        
        if (total > 0) {
            for (UUID playerId : teamPlayers) {
                double contribution = playerData.getOrDefault(playerId, 0.0) / total;
                contributions.put(playerId, contribution);
            }
        } else {
            // No data, split equally
            double equalShare = 1.0 / teamPlayers.size();
            for (UUID playerId : teamPlayers) {
                contributions.put(playerId, equalShare);
            }
        }
        
        return contributions;
    }
    
    /**
     * Check if goal is an opponent goal where each player gets equal (0.5) credit
     */
    private boolean isOpponentGoalWithEqualCredit(Goal goal) {
        return goal instanceof OpponentDiesGoal ||
               goal instanceof OpponentDies3TimesGoal ||
               goal instanceof OpponentEatsFoodGoal ||
               goal instanceof OpponentHitByArrowGoal ||
               goal instanceof OpponentHitByWindChargeGoal ||
               goal instanceof OpponentObtainsCraftingTableGoal ||
               goal instanceof OpponentObtainsObsidianGoal ||
               goal instanceof OpponentObtainsSeedsGoal ||
               goal instanceof OpponentTakes100DamageGoal ||
               goal instanceof OpponentTakesFallDamageGoal ||
               goal instanceof OpponentCatchesOnFireGoal ||
               goal instanceof OpponentTouchesWaterGoal;
    }
    
    /**
     * Get final game time in a readable format
     */
    public String getGameTimeFormatted() {
        long totalSeconds = lockout.getTicks() / 20; // Convert ticks to seconds
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Get total goal contribution for a player (sum of all their goal contributions)
     */
    public double getPlayerTotalContribution(UUID playerId) {
        Map<Goal, Double> contributions = playerGoalContributions.get(playerId);
        if (contributions == null) return 0.0;
        
        double total = contributions.values().stream().mapToDouble(Double::doubleValue).sum();
        return Math.round(total * 100.0) / 100.0;
    }
    
    public void setWinners(List<LockoutTeam> winners) {
        this.winners = winners;
    }
    
    /**
     * Send clickable buttons for viewing and downloading statistics
     */
    public void displayInChat() {
        PlayerManager playerManager = LockoutServer.server.getPlayerManager();
        
        // Create clickable [View Statistics] button
        Text viewButton = Text.literal("[View Statistics]")
            .formatted(Formatting.GREEN, Formatting.BOLD)
            .styled(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/GameStatistics view"))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to view game statistics")))
            );
        
        // Create clickable [Download Statistics] button
        Text downloadButton = Text.literal("[Download Statistics]")
            .formatted(Formatting.AQUA, Formatting.BOLD)
            .styled(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/GameStatistics download"))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to save statistics file")))
            );
        
        // Send buttons
        playerManager.broadcast(Text.literal("").formatted(Formatting.GOLD), false);
        playerManager.broadcast(
            Text.literal("Game Over! ").formatted(Formatting.YELLOW)
                .append(viewButton)
                .append(Text.literal("  "))
                .append(downloadButton),
            false
        );
        playerManager.broadcast(Text.literal("").formatted(Formatting.GOLD), false);
    }
    
    /**
     * Display full statistics in chat
     */
    public void showFullStatistics() {
        showFullStatistics(null);
    }
    
    public void showFullStatistics(ServerPlayerEntity targetPlayer) {
        calculateGoalContributions();
        
        PlayerManager playerManager = LockoutServer.server.getPlayerManager();
        
        // Header
        sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        sendMessage(playerManager, targetPlayer, Text.literal("========== Game Statistics ==========").formatted(Formatting.GOLD, Formatting.BOLD));
        sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        
        // Winner(s)
        if (!winners.isEmpty()) {
            if (winners.size() == 1) {
                LockoutTeam winner = winners.get(0);
                sendMessage(playerManager, targetPlayer,
                    Text.literal("Winner: ").formatted(Formatting.YELLOW, Formatting.BOLD)
                        .append(Text.literal(winner.getDisplayName()).formatted(winner.getColor(), Formatting.BOLD))
                );
            } else {
                StringBuilder winnerNames = new StringBuilder();
                for (int i = 0; i < winners.size(); i++) {
                    if (i > 0) winnerNames.append(", ");
                    winnerNames.append(winners.get(i).getDisplayName());
                }
                sendMessage(playerManager, targetPlayer,
                    Text.literal("Winners (Tie): ").formatted(Formatting.YELLOW, Formatting.BOLD)
                        .append(Text.literal(winnerNames.toString()).formatted(Formatting.GOLD, Formatting.BOLD))
                );
            }
            
            // Display score
            // Find winning team (highest score)
            int maxScore = lockout.getTeams().stream().mapToInt(LockoutTeam::getPoints).max().orElse(0);
            LockoutTeam winningTeam = null;
            LockoutTeam losingTeam = null;
            
            for (LockoutTeam team : lockout.getTeams()) {
                if (team.getPoints() == maxScore) {
                    winningTeam = team;
                } else {
                    losingTeam = team;
                }
            }
            
            if (winningTeam != null && losingTeam != null) {
                Text scoreText = Text.literal("Score: ").formatted(Formatting.YELLOW)
                    .append(Text.literal(winningTeam.getDisplayName() + " ").formatted(winningTeam.getColor()))
                    .append(Text.literal(String.valueOf(winningTeam.getPoints())).formatted(Formatting.GREEN))
                    .append(Text.literal(" - ").formatted(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(losingTeam.getPoints())).formatted(Formatting.RED))
                    .append(Text.literal(" " + losingTeam.getDisplayName()).formatted(losingTeam.getColor()));
                
                sendMessage(playerManager, targetPlayer, scoreText);
            }
            
            sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        }
        
        // Game Time
        sendMessage(playerManager, targetPlayer, Text.literal("Final Game Time: ").formatted(Formatting.YELLOW)
                .append(Text.literal(getGameTimeFormatted()).formatted(Formatting.WHITE)));
        sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        
        // Goals Completed by Team
        sendMessage(playerManager, targetPlayer, Text.literal("Goals Completed:").formatted(Formatting.YELLOW, Formatting.BOLD));
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal != null && goal.isCompleted()) {
                LockoutTeam team = goal.getCompletedTeam();
                String teamDisplay = team != null ? team.getDisplayName() : "Unknown";
                Formatting teamColor = team != null ? team.getColor() : Formatting.WHITE;
                
                sendMessage(playerManager, targetPlayer, Text.literal("  • ").formatted(Formatting.GRAY)
                        .append(Text.literal(goal.getGoalName()).formatted(Formatting.WHITE))
                        .append(Text.literal(" - ").formatted(Formatting.GRAY))
                        .append(Text.literal(teamDisplay).formatted(teamColor)));
            }
        }
        sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        
        // Player Statistics
        sendMessage(playerManager, targetPlayer, Text.literal("Player Statistics:").formatted(Formatting.YELLOW, Formatting.BOLD));
        
        for (LockoutTeam team : lockout.getTeams()) {
            if (!(team instanceof LockoutTeamServer serverTeam)) continue;
            
            for (UUID playerId : serverTeam.getPlayers()) {
                String playerName = serverTeam.getPlayerName(playerId);
                int deaths = playerDeaths.getOrDefault(playerId, 0);
                int kills = playerKills.getOrDefault(playerId, 0);
                double contribution = getPlayerTotalContribution(playerId);
                
                sendMessage(playerManager, targetPlayer, Text.literal("  " + playerName + ":").formatted(team.getColor(), Formatting.BOLD));
                sendMessage(playerManager, targetPlayer, Text.literal("    Deaths: ").formatted(Formatting.GRAY)
                        .append(Text.literal(String.valueOf(deaths)).formatted(Formatting.WHITE)));
                sendMessage(playerManager, targetPlayer, Text.literal("    Player Kills: ").formatted(Formatting.GRAY)
                        .append(Text.literal(String.valueOf(kills)).formatted(Formatting.WHITE)));
                sendMessage(playerManager, targetPlayer, Text.literal("    Goal Contribution: ").formatted(Formatting.GRAY)
                        .append(Text.literal(String.format("%.2f", contribution)).formatted(Formatting.WHITE)));
            }
        }
        
        sendMessage(playerManager, targetPlayer, Text.literal("").formatted(Formatting.GOLD));
        sendMessage(playerManager, targetPlayer, Text.literal("====================================").formatted(Formatting.GOLD, Formatting.BOLD));
    }
    
    /**
     * Helper method to send message to a specific player or broadcast to all
     */
    private void sendMessage(PlayerManager playerManager, ServerPlayerEntity targetPlayer, Text message) {
        if (targetPlayer != null) {
            targetPlayer.sendMessage(message, false);
        } else {
            playerManager.broadcast(message, false);
        }
    }
    
    /**
     * Generate statistics content as a string for client-side download
     * @return A tuple containing the filename and content
     */
    public String[] generateStatisticsContent() {
        calculateGoalContributions();
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String filename = "game_" + timestamp + ".txt";
        
        StringBuilder content = new StringBuilder();
        content.append("========== Lockout Game Statistics ==========\n");
        content.append("\n");
        content.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        
        // Winner(s)
        if (!winners.isEmpty()) {
            if (winners.size() == 1) {
                content.append("Winner: ").append(winners.get(0).getDisplayName()).append("\n");
            } else {
                StringBuilder winnerNames = new StringBuilder();
                for (int i = 0; i < winners.size(); i++) {
                    if (i > 0) winnerNames.append(", ");
                    winnerNames.append(winners.get(i).getDisplayName());
                }
                content.append("Winners (Tie): ").append(winnerNames).append("\n");
            }
            
            // Display score
            content.append("Score: ");
            for (int i = 0; i < lockout.getTeams().size(); i++) {
                LockoutTeam team = lockout.getTeams().get(i);
                if (i > 0) content.append(" - ");
                content.append(team.getDisplayName()).append(": ").append(team.getPoints());
            }
            content.append("\n");
        }
        
        content.append("Final Game Time: ").append(getGameTimeFormatted()).append("\n");
        content.append("\n");
        
        // Goals Completed
        content.append("===== Goals Completed =====\n");
        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal != null && goal.isCompleted()) {
                LockoutTeam team = goal.getCompletedTeam();
                String teamDisplay = team != null ? team.getDisplayName() : "Unknown";
                content.append("  • ").append(goal.getGoalName()).append(" - ").append(teamDisplay).append("\n");
                
                // Show individual player contributions for this goal
                if (team instanceof LockoutTeamServer serverTeam) {
                    for (UUID playerId : serverTeam.getPlayers()) {
                        Map<Goal, Double> contributions = playerGoalContributions.get(playerId);
                        if (contributions != null && contributions.containsKey(goal)) {
                            double contribution = contributions.get(goal);
                            if (contribution > 0) {
                                String playerName = serverTeam.getPlayerName(playerId);
                                content.append("      ").append(playerName).append(": ")
                                       .append(String.format("%.2f", contribution)).append("\n");
                            }
                        }
                    }
                }
            }
        }
        content.append("\n");
        
        // Player Statistics
        content.append("===== Player Statistics =====\n");
        for (LockoutTeam team : lockout.getTeams()) {
            if (!(team instanceof LockoutTeamServer serverTeam)) continue;
            
            content.append("\n").append(team.getDisplayName()).append(":\n");
            for (UUID playerId : serverTeam.getPlayers()) {
                String playerName = serverTeam.getPlayerName(playerId);
                int deaths = playerDeaths.getOrDefault(playerId, 0);
                int kills = playerKills.getOrDefault(playerId, 0);
                double contribution = getPlayerTotalContribution(playerId);
                
                content.append("  ").append(playerName).append(":\n");
                content.append("    Deaths: ").append(deaths).append("\n");
                content.append("    Player Kills: ").append(kills).append("\n");
                content.append("    Goal Contribution: ").append(String.format("%.2f", contribution)).append("\n");
                content.append("\n");
            }
        }
        
        content.append("===========================================\n");
        
        return new String[]{filename, content.toString()};
    }
    
    /**
     * Save statistics to a file
     * @return The absolute path of the saved file, or null if failed
     */
    public String saveToFile() {
        calculateGoalContributions();
        
        File statisticsDir = new File("lockout-statistics");
        if (!statisticsDir.exists()) {
            statisticsDir.mkdirs();
        }
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File statsFile = new File(statisticsDir, "game_" + timestamp + ".txt");
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(statsFile))) {
            writer.write("========== Lockout Game Statistics ==========\n");
            writer.write("\n");
            writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            
            // Winner(s)
            if (!winners.isEmpty()) {
                if (winners.size() == 1) {
                    writer.write("Winner: " + winners.get(0).getDisplayName() + "\n");
                } else {
                    StringBuilder winnerNames = new StringBuilder();
                    for (int i = 0; i < winners.size(); i++) {
                        if (i > 0) winnerNames.append(", ");
                        winnerNames.append(winners.get(i).getDisplayName());
                    }
                    writer.write("Winners (Tie): " + winnerNames + "\n");
                }
                
                // Display score
                writer.write("Score: ");
                for (int i = 0; i < lockout.getTeams().size(); i++) {
                    LockoutTeam team = lockout.getTeams().get(i);
                    if (i > 0) writer.write(" - ");
                    writer.write(team.getDisplayName() + ": " + team.getPoints());
                }
                writer.write("\n");
            }
            
            writer.write("Final Game Time: " + getGameTimeFormatted() + "\n");
            writer.write("\n");
            
            // Goals Completed
            writer.write("===== Goals Completed =====\n");
            for (Goal goal : lockout.getBoard().getGoals()) {
                if (goal != null && goal.isCompleted()) {
                    LockoutTeam team = goal.getCompletedTeam();
                    String teamDisplay = team != null ? team.getDisplayName() : "Unknown";
                    writer.write("  • " + goal.getGoalName() + " - " + teamDisplay + "\n");
                }
            }
            writer.write("\n");
            
            // Player Statistics
            writer.write("===== Player Statistics =====\n");
            for (LockoutTeam team : lockout.getTeams()) {
                if (!(team instanceof LockoutTeamServer serverTeam)) continue;
                
                writer.write("\n" + team.getDisplayName() + ":\n");
                for (UUID playerId : serverTeam.getPlayers()) {
                    String playerName = serverTeam.getPlayerName(playerId);
                    int deaths = playerDeaths.getOrDefault(playerId, 0);
                    int kills = playerKills.getOrDefault(playerId, 0);
                    double contribution = getPlayerTotalContribution(playerId);
                    
                    writer.write("  " + playerName + ":\n");
                    writer.write("    Deaths: " + deaths + "\n");
                    writer.write("    Player Kills: " + kills + "\n");
                    writer.write("    Goal Contribution: " + String.format("%.2f", contribution) + "\n");
                    writer.write("\n");
                }
            }
            
            writer.write("===========================================\n");
            
            Lockout.log("Statistics saved to: " + statsFile.getAbsolutePath());
            return statsFile.getAbsolutePath();
        } catch (IOException e) {
            Lockout.error(e);
            return null;
        }
    }
}
