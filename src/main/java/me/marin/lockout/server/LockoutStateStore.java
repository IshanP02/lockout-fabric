package me.marin.lockout.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.marin.lockout.CompassItemHandler;
import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.client.LockoutBoard;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import oshi.util.tuples.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LockoutStateStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "lockout_state.json";
    private static String lastSavedJson = null;

    private LockoutStateStore() {
    }

    public static void save(MinecraftServer server) {
        if (server == null) return;

        SaveData data = buildSaveData();
        Path path = getStatePath(server);

        if (data == null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                Lockout.error(e);
            }
            lastSavedJson = null;
            return;
        }

        String json = GSON.toJson(data);
        if (json.equals(lastSavedJson)) return;

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json, StandardCharsets.UTF_8);
            lastSavedJson = json;
        } catch (IOException e) {
            Lockout.error(e);
        }
    }

    public static void load(MinecraftServer server) {
        if (server == null) return;

        Path path = getStatePath(server);
        if (!Files.exists(path)) {
            lastSavedJson = null;
            return;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                lastSavedJson = null;
                return;
            }

            SaveData data = GSON.fromJson(json, SaveData.class);
            applySaveData(server, data);
            lastSavedJson = json;
        } catch (Exception e) {
            Lockout.error(e);
        }
    }

    private static Path getStatePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(FILE_NAME);
    }

    private static SaveData buildSaveData() {
        Lockout lockout = LockoutServer.lockout;
        if (lockout == null) return null;

        SaveData data = new SaveData();
        data.ticks = lockout.getTicks();
        data.started = lockout.hasStarted();
        data.running = lockout.isRunning();

        data.serverPicks = new ArrayList<>(LockoutServer.SERVER_PICKS);
        data.serverBans = new ArrayList<>(LockoutServer.SERVER_BANS);
        data.goalToPlayerMap = new HashMap<>(LockoutServer.SERVER_GOAL_TO_PLAYER_MAP);
        data.boardType = LockoutServer.boardType;
        data.boardTypeExcludedGoals = new ArrayList<>(LockoutServer.boardTypeExcludedGoals);

        data.teams = new ArrayList<>();
        for (LockoutTeam team : lockout.getTeams()) {
            if (!(team instanceof LockoutTeamServer teamServer)) continue;

            TeamData teamData = new TeamData();
            teamData.color = team.getColor() == null ? Formatting.WHITE.asString() : team.getColor().asString();
            teamData.points = team.getPoints();
            teamData.playerNames = new ArrayList<>(team.getPlayerNames());
            teamData.playerUuids = new ArrayList<>();
            teamData.uuidToName = new LinkedHashMap<>();

            for (UUID uuid : teamServer.getPlayers()) {
                String name = teamServer.getPlayerName(uuid);
                teamData.playerUuids.add(uuid.toString());
                if (name != null && !name.isEmpty()) {
                    teamData.uuidToName.put(uuid.toString(), name);
                }
            }

            data.teams.add(teamData);
        }

        data.goals = new ArrayList<>();
        for (Goal goal : lockout.getBoard().getGoals()) {
            GoalData goalData = new GoalData();
            if (goal == null) {
                goalData.id = GoalType.NULL;
                goalData.data = "null";
                goalData.completedTeamIndex = -1;
                goalData.completedMessage = "";
            } else {
                goalData.id = goal.getId();
                goalData.data = goal.getData();
                goalData.completedTeamIndex = lockout.getTeams().indexOf(goal.getCompletedTeam());
                goalData.completedMessage = goal.getCompletedMessage();
            }
            data.goals.add(goalData);
        }

        return data;
    }

    private static void applySaveData(MinecraftServer server, SaveData data) {
        if (data == null || data.teams == null || data.goals == null || data.teams.isEmpty() || data.goals.isEmpty()) {
            return;
        }

        List<LockoutTeamServer> teams = new ArrayList<>();
        for (TeamData teamData : data.teams) {
            if (teamData == null) continue;

            List<String> playerNames = teamData.playerNames == null ? new ArrayList<>() : new ArrayList<>(teamData.playerNames);
            List<UUID> playerIds = new ArrayList<>();
            Map<UUID, String> playerNameMap = new HashMap<>();

            if (teamData.playerUuids != null) {
                for (String uuidString : teamData.playerUuids) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        playerIds.add(uuid);

                        String mappedName = teamData.uuidToName == null ? null : teamData.uuidToName.get(uuidString);
                        if (mappedName != null && !mappedName.isBlank()) {
                            playerNameMap.put(uuid, mappedName);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            // For very old state files, derive names from map if the list is empty.
            if (playerNames.isEmpty() && !playerNameMap.isEmpty()) {
                playerNames.addAll(playerNameMap.values());
            }

            Formatting color = Formatting.byName(teamData.color);
            if (color == null) {
                color = Formatting.WHITE;
            }

            teams.add(new LockoutTeamServer(
                    playerNames,
                    color,
                    server,
                    playerIds,
                    playerNameMap,
                    Math.max(0, teamData.points)
            ));
        }

        if (teams.isEmpty()) {
            return;
        }

        List<Pair<String, String>> boardGoals = new ArrayList<>();
        for (GoalData goalData : data.goals) {
            if (goalData == null || goalData.id == null || goalData.id.isBlank()) {
                boardGoals.add(new Pair<>(GoalType.NULL, "null"));
                continue;
            }
            boardGoals.add(new Pair<>(goalData.id, goalData.data == null ? "null" : goalData.data));
        }

        LockoutBoard board;
        try {
            board = new LockoutBoard(boardGoals);
        } catch (Exception e) {
            Lockout.error(e);
            return;
        }

        Lockout restored = new Lockout(board, teams);
        restored.setTicks(data.ticks);
        restored.setStarted(data.started);
        restored.setRunning(data.running);

        List<Goal> restoredGoals = restored.getBoard().getGoals();
        for (int i = 0; i < Math.min(restoredGoals.size(), data.goals.size()); i++) {
            Goal goal = restoredGoals.get(i);
            GoalData goalData = data.goals.get(i);

            if (goal == null || goalData == null) continue;
            if (goalData.completedTeamIndex < 0 || goalData.completedTeamIndex >= teams.size()) continue;

            LockoutTeam team = teams.get(goalData.completedTeamIndex);
            goal.setCompleted(true, team, goalData.completedMessage == null ? "" : goalData.completedMessage);
        }

        LockoutServer.lockout = restored;
        LockoutServer.SERVER_PICKS.clear();
        LockoutServer.SERVER_PICKS.addAll(data.serverPicks == null ? List.of() : data.serverPicks);
        LockoutServer.SERVER_BANS.clear();
        LockoutServer.SERVER_BANS.addAll(data.serverBans == null ? List.of() : data.serverBans);
        LockoutServer.SERVER_GOAL_TO_PLAYER_MAP.clear();
        LockoutServer.SERVER_GOAL_TO_PLAYER_MAP.putAll(data.goalToPlayerMap == null ? Map.of() : data.goalToPlayerMap);
        LockoutServer.boardType = data.boardType;
        LockoutServer.boardTypeExcludedGoals = data.boardTypeExcludedGoals == null ? new ArrayList<>() : new ArrayList<>(data.boardTypeExcludedGoals);

        List<UUID> allLockoutPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .toList();

        if (!allLockoutPlayers.isEmpty() && !restored.isSoloBlackout()) {
            LockoutServer.compassHandler = new CompassItemHandler(allLockoutPlayers, server.getPlayerManager());
        } else {
            LockoutServer.compassHandler = null;
        }

        Lockout.log("Restored lockout state from save file.");
    }

    private static class SaveData {
        long ticks;
        boolean started;
        boolean running;
        String boardType;
        List<String> boardTypeExcludedGoals;
        List<String> serverPicks;
        List<String> serverBans;
        Map<String, String> goalToPlayerMap;
        List<TeamData> teams;
        List<GoalData> goals;
    }

    private static class TeamData {
        String color;
        int points;
        List<String> playerNames;
        List<String> playerUuids;
        Map<String, String> uuidToName;
    }

    private static class GoalData {
        String id;
        String data;
        int completedTeamIndex;
        String completedMessage;
    }
}
