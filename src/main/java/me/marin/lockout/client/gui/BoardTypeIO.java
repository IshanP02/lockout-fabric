package me.marin.lockout.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.marin.lockout.Lockout;
import me.marin.lockout.json.JSONBoardType;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages saving and loading custom BoardTypes from JSON files.
 * Similar to BoardBuilderIO but for BoardType configurations.
 */
public class BoardTypeIO {

    public static final Path DIRECTORY = MinecraftClient.getInstance().runDirectory.toPath().resolve("lockout-boardtypes");
    public static final String FILE_EXTENSION = ".json";

    public static final BoardTypeIO INSTANCE = new BoardTypeIO();

    public BoardTypeIO() {
        if (!Files.exists(DIRECTORY)) {
            try {
                Files.createDirectories(DIRECTORY);
            } catch (IOException e) {
                Lockout.error(e);
            }
        }
    }

    /**
     * Saves a custom BoardType to a JSON file.
     * 
     * @param boardType The BoardType configuration to save
     * @throws IOException If file operations fail
     */
    public void saveBoardType(JSONBoardType boardType) throws IOException {
        Path boardTypePath = DIRECTORY.resolve(boardType.name + FILE_EXTENSION);
        
        // Delete existing file if it exists
        if (Files.exists(boardTypePath)) {
            Files.delete(boardTypePath);
        }
        
        Files.createFile(boardTypePath);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String jsonString = gson.toJson(boardType);
        Files.writeString(boardTypePath, jsonString);
    }

    /**
     * Gets a list of all saved custom BoardType names.
     * 
     * @return List of BoardType names (without file extension)
     * @throws IOException If directory listing fails
     */
    public List<String> getSavedBoardTypes() throws IOException {
        if (!Files.exists(DIRECTORY)) {
            return new ArrayList<>();
        }
        return Files.list(DIRECTORY)
                .map(p -> StringUtils.removeEnd(p.getFileName().toString(), FILE_EXTENSION))
                .toList();
    }

    /**
     * Gets the file path for a BoardType by name.
     * 
     * @param name The name of the BoardType
     * @return Path to the BoardType file
     */
    public Path getBoardTypePath(String name) {
        return DIRECTORY.resolve(name + FILE_EXTENSION);
    }

    /**
     * Reads a custom BoardType from file.
     * 
     * @param name The name of the BoardType to read
     * @return The loaded JSONBoardType
     * @throws IOException If file reading fails
     */
    public JSONBoardType readBoardType(String name) throws IOException {
        Gson gson = new Gson();
        return gson.fromJson(Files.readString(getBoardTypePath(name)), JSONBoardType.class);
    }

    /**
     * Deletes a custom BoardType file.
     * 
     * @param name The name of the BoardType to delete
     * @return true if the file was deleted, false otherwise
     */
    public boolean deleteBoardType(String name) {
        try {
            Path path = getBoardTypePath(name);
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            Lockout.error(e);
            return false;
        }
    }

    /**
     * Checks if a BoardType with the given name exists.
     * 
     * @param name The name to check
     * @return true if a BoardType with this name exists
     */
    public boolean boardTypeExists(String name) {
        return Files.exists(getBoardTypePath(name));
    }

    /**
     * Changes the board type name if necessary. If board type name isn't specified or the name already exists,
     * append (1), (2), or whatever first available name is.
     *  
     * @param boardTypeName Name of the board type
     * @return Changed name of the board type
     */
    public String getSuitableName(String boardTypeName) throws IOException {
        if (boardTypeName == null || boardTypeName.trim().isEmpty()) {
            boardTypeName = "Custom BoardType";
        }
        
        boolean exists = false;
        List<String> savedBoardTypes = getSavedBoardTypes();
        for (String savedBoardType : savedBoardTypes) {
            if (savedBoardType.equals(boardTypeName)) {
                exists = true;
                break;
            }
        }
        
        if (!exists) return boardTypeName;

        int num = 1;
        while (savedBoardTypes.contains(boardTypeName + " (" + num + ")")) {
            num++;
        }
        return boardTypeName + " (" + num + ")";
    }

}
