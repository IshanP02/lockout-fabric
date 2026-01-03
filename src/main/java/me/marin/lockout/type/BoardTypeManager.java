package me.marin.lockout.type;

import me.marin.lockout.Lockout;
import me.marin.lockout.client.gui.BoardTypeIO;
import me.marin.lockout.json.JSONBoardType;

import java.io.IOException;
import java.util.*;

public class BoardTypeManager {

    public static final BoardTypeManager INSTANCE = new BoardTypeManager();

    private final Map<String, JSONBoardType> customBoardTypes = new HashMap<>();

    private BoardTypeManager() {}

    public List<String> getAllBoardTypeNames() {
        try {
            return BoardTypeIO.INSTANCE.getSavedBoardTypes();
        } catch (IOException e) {
            Lockout.error(e);
            return new ArrayList<>();
        }
    }

    public boolean isGoalExcluded(String boardTypeName, String goalId) {
        JSONBoardType customType = getCustomBoardType(boardTypeName);
        return customType != null 
            && customType.excludedGoals != null 
            && customType.excludedGoals.contains(goalId);
    }

    public JSONBoardType getCustomBoardType(String name) {
        if (customBoardTypes.containsKey(name)) {
            return customBoardTypes.get(name);
        }
        
        try {
            JSONBoardType boardType = BoardTypeIO.INSTANCE.readBoardType(name);
            customBoardTypes.put(name, boardType);
            return boardType;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean boardTypeExists(String name) {
        return BoardTypeIO.INSTANCE.boardTypeExists(name);
    }

    public void clearCache() {
        customBoardTypes.clear();
    }

    public void reloadBoardType(String name) {
        customBoardTypes.remove(name);
        getCustomBoardType(name);
    }

}
