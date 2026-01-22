package me.marin.lockout.server;

import lombok.Getter;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;

import java.util.*;

@Getter
public class PickBanSession {
    private final int maxRounds;
    
    private final String team1Name;
    private final String team2Name;
    private final Team team1;
    private final Team team2;
    private final MinecraftServer server;
    
    private int currentRound = 1;
    private boolean isTeam1Turn = true;
    private int selectionLimit = 2; // Default 2 picks and 2 bans per round
    
    // Track all locked picks/bans by both teams
    private final Set<String> allLockedPicks = new HashSet<>();
    private final Set<String> allLockedBans = new HashSet<>();
    
    // Track which team locked which goals
    private final Map<String, String> goalToTeamMap = new HashMap<>();
    
    // Track which player selected which goal (for player heads in GUI)
    private final Map<String, String> goalToPlayerMap = new HashMap<>();
    
    // Temporary selections before lock
    private final List<String> pendingPicks = new ArrayList<>();
    private final List<String> pendingBans = new ArrayList<>();
    
    // Team-specific storage
    private final Map<String, List<String>> teamPicks = new HashMap<>();
    private final Map<String, List<String>> teamBans = new HashMap<>();
    
    public PickBanSession(Team team1, Team team2, MinecraftServer server, int maxRounds) {
        this.team1 = team1;
        this.team2 = team2;
        this.team1Name = team1.getName();
        this.team2Name = team2.getName();
        this.server = server;
        this.maxRounds = maxRounds;
        
        teamPicks.put(team1Name, new ArrayList<>());
        teamPicks.put(team2Name, new ArrayList<>());
        teamBans.put(team1Name, new ArrayList<>());
        teamBans.put(team2Name, new ArrayList<>());
    }
    
    /**
     * Get the currently active team
     */
    public Team getCurrentActiveTeam() {
        return isTeam1Turn ? team1 : team2;
    }
    
    /**
     * Get the currently active team name
     */
    public String getCurrentActiveTeamName() {
        return isTeam1Turn ? team1Name : team2Name;
    }
    
    /**
     * Check if a player is on the currently active team
     */
    public boolean isPlayerOnActiveTeam(String playerName) {
        Team activeTeam = getCurrentActiveTeam();
        return activeTeam.getPlayerList().contains(playerName);
    }
    
    /**
     * Check if the current round is a ban round (odd rounds)
     */
    public boolean isBanRound() {
        return currentRound % 2 == 1;
    }
    
    /**
     * Check if the current round is a pick round (even rounds)
     */
    public boolean isPickRound() {
        return currentRound % 2 == 0;
    }
    
    /**
     * Check if a goal can be picked (not already picked by either team)
     */
    public boolean canPickGoal(String goalId) {
        return !allLockedPicks.contains(goalId);
    }
    
    /**
     * Check if a goal can be banned (not already banned by either team)
     */
    public boolean canBanGoal(String goalId) {
        return !allLockedBans.contains(goalId);
    }
    
    /**
     * Check if more picks can be added
     */
    public boolean canAddMorePicks() {
        // Can't add picks during ban rounds
        if (isBanRound()) return false;
        return pendingPicks.size() < selectionLimit;
    }
    
    /**
     * Check if more bans can be added
     */
    public boolean canAddMoreBans() {
        // Can't add bans during pick rounds
        if (isPickRound()) return false;
        return pendingBans.size() < selectionLimit;
    }
    
    /**
     * Check if current team has selected enough picks/bans to lock
     */
    public boolean canLockSelections() {
        if (isBanRound()) {
            // Ban rounds: only check bans
            return pendingBans.size() == selectionLimit;
        } else {
            // Pick rounds: only check picks
            return pendingPicks.size() == selectionLimit;
        }
    }
    
    /**
     * Add a pending pick
     */
    public boolean addPendingPick(String goalId) {
        if (!canAddMorePicks()) return false;
        if (!canPickGoal(goalId)) return false;
        if (pendingPicks.contains(goalId)) return false;
        
        pendingPicks.add(goalId);
        pendingBans.remove(goalId); // Remove from bans if it was there
        return true;
    }
    
    /**
     * Add a pending ban
     */
    public boolean addPendingBan(String goalId) {
        if (!canAddMoreBans()) return false;
        if (!canBanGoal(goalId)) return false;
        if (pendingBans.contains(goalId)) return false;
        
        pendingBans.add(goalId);
        pendingPicks.remove(goalId); // Remove from picks if it was there
        return true;
    }
    
    /**
     * Remove a pending pick
     */
    public void removePendingPick(String goalId) {
        pendingPicks.remove(goalId);
    }
    
    /**
     * Remove a pending ban
     */
    public void removePendingBan(String goalId) {
        pendingBans.remove(goalId);
    }
    
    /**
     * Check if a goal is locked by any team
     */
    public boolean isGoalLocked(String goalId) {
        return goalToTeamMap.containsKey(goalId);
    }
    
    /**
     * Get the team name that locked a goal
     */
    public String getTeamThatLockedGoal(String goalId) {
        return goalToTeamMap.get(goalId);
    }
    
    /**
     * Set the player who selected a goal
     */
    public void setGoalPlayer(String goalId, String playerName) {
        goalToPlayerMap.put(goalId, playerName);
    }
    
    /**
     * Get the player who selected a goal
     */
    public String getGoalPlayer(String goalId) {
        return goalToPlayerMap.get(goalId);
    }
    
    /**
     * Get all goal-to-player mappings
     */
    public Map<String, String> getGoalToPlayerMap() {
        return new HashMap<>(goalToPlayerMap);
    }
    
    /**
     * Lock current team's selections and advance to next turn
     */
    public void lockCurrentSelections() {
        String activeTeamName = getCurrentActiveTeamName();
        
        // Add pending picks/bans to global tracking
        for (String goalId : pendingPicks) {
            allLockedPicks.add(goalId);
            goalToTeamMap.put(goalId, activeTeamName);
        }
        for (String goalId : pendingBans) {
            allLockedBans.add(goalId);
            goalToTeamMap.put(goalId, activeTeamName);
        }
        
        // Store in team-specific storage
        teamPicks.get(activeTeamName).addAll(pendingPicks);
        teamBans.get(activeTeamName).addAll(pendingBans);
        
        // Clear pending
        pendingPicks.clear();
        pendingBans.clear();
        
        // Advance turn
        advanceTurn();
    }
    
    /**
     * Move to next turn/round (snake draft style)
     * Round 1: T1 → T2, Round 2: T2 → T1, Round 3: T1 → T2, etc.
     */
    private void advanceTurn() {
        boolean isOddRound = (currentRound % 2 == 1);
        
        if (isOddRound) {
            // Odd round: T1 → T2
            if (isTeam1Turn) {
                // Team 1 just finished, now Team 2's turn (same round)
                isTeam1Turn = false;
            } else {
                // Team 2 just finished, round complete
                currentRound++;
                // Next round is even, so Team 2 goes first
                isTeam1Turn = false;
            }
        } else {
            // Even round: T2 → T1
            if (!isTeam1Turn) {
                // Team 2 just finished, now Team 1's turn (same round)
                isTeam1Turn = true;
            } else {
                // Team 1 just finished, round complete
                currentRound++;
                // Next round is odd, so Team 1 goes first
                isTeam1Turn = true;
            }
        }
    }
    
    /**
     * Check if the session is complete
     */
    public boolean isComplete() {
        return currentRound > maxRounds;
    }
    
    /**
     * Set the selection limit (picks and bans per round)
     */
    public void setSelectionLimit(int limit) {
        this.selectionLimit = limit;
    }
    
    /**
     * Get all picks for a specific team
     */
    public List<String> getTeamPicks(String teamName) {
        return new ArrayList<>(teamPicks.getOrDefault(teamName, new ArrayList<>()));
    }
    
    /**
     * Get all bans for a specific team
     */
    public List<String> getTeamBans(String teamName) {
        return new ArrayList<>(teamBans.getOrDefault(teamName, new ArrayList<>()));
    }
    
    /**
     * Clear all pending selections (used when cancelling)
     */
    public void clearPendingSelections() {
        pendingPicks.clear();
        pendingBans.clear();
    }
}
