package me.marin.lockout.type;

import me.marin.lockout.Lockout;
import me.marin.lockout.client.gui.BoardTypeIO;
import me.marin.lockout.json.JSONBoardType;

import java.io.IOException;
import java.util.*;

/**
 * Manager class to handle both built-in and custom BoardTypes.
 * This class bridges the gap between the enum system and the file-based custom types.
 * To do: Remove enum system BoardTypes
 */
public class BoardTypeManager {

    public static final BoardTypeManager INSTANCE = new BoardTypeManager();

    private final Map<String, JSONBoardType> customBoardTypes = new HashMap<>();

    private BoardTypeManager() {}

    /**
     * Gets all available BoardType names (both built-in and custom).
     */
    public List<String> getAllBoardTypeNames() {
        List<String> names = new ArrayList<>();
        
        // Add built-in enum types
        for (BoardType type : BoardType.values()) {
            names.add(type.name());
        }
        
        // Add custom types
        try {
            names.addAll(BoardTypeIO.INSTANCE.getSavedBoardTypes());
        } catch (IOException e) {
            Lockout.error(e);
        }
        
        return names;
    }

    /**
     * Checks if a goal should be excluded based on the given BoardType name.
     */
    public boolean isGoalExcluded(String boardTypeName, String goalId) {
        // Check if it's a built-in enum type
        try {
            BoardType enumType = BoardType.valueOf(boardTypeName.toUpperCase());
            return enumType.isGoalExcluded(goalId);
        } catch (IllegalArgumentException e) {
            // Not a built-in type, check custom types
            JSONBoardType customType = getCustomBoardType(boardTypeName);
            return customType != null 
                && customType.excludedGoals != null 
                && customType.excludedGoals.contains(goalId);
        }
    }

    /**
     * Gets a custom BoardType from cache or loads it from file.
     * 
     * @param name The name of the custom BoardType
     * @return The JSONBoardType, or null if not found
     */
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

    /**
     * Checks if a BoardType exists (either built-in or custom).
     * 
     * @param name The name to check
     * @return true if the BoardType exists
     */
    public boolean boardTypeExists(String name) {
        // Check built-in types
        try {
            BoardType.valueOf(name.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // Check custom types
            return BoardTypeIO.INSTANCE.boardTypeExists(name);
        }
    }

    /**
     * Checks if a BoardType name refers to a custom (not built-in) type.
     * 
     * @param name The name to check
     * @return true if it's a custom BoardType
     */
    public boolean isCustomBoardType(String name) {
        try {
            BoardType.valueOf(name.toUpperCase());
            return false; // It's a built-in type
        } catch (IllegalArgumentException e) {
            return BoardTypeIO.INSTANCE.boardTypeExists(name);
        }
    }

    /**
     * Clears the cache of loaded custom BoardTypes.
     * Call this after creating, updating, or deleting custom BoardTypes.
     */
    public void clearCache() {
        customBoardTypes.clear();
    }

    /**
     * Reloads a specific custom BoardType from file.
     * 
     * @param name The name of the custom BoardType to reload
     */
    public void reloadBoardType(String name) {
        customBoardTypes.remove(name);
        getCustomBoardType(name);
    }

}
