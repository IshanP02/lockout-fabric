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

    public void saveBoardType(JSONBoardType boardType) throws IOException {
        Path boardTypePath = DIRECTORY.resolve(boardType.name + FILE_EXTENSION);
        
        if (Files.exists(boardTypePath)) {
            Files.delete(boardTypePath);
        }
        
        Files.createFile(boardTypePath);
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String jsonString = gson.toJson(boardType);
        Files.writeString(boardTypePath, jsonString);
    }

    public List<String> getSavedBoardTypes() throws IOException {
        if (!Files.exists(DIRECTORY)) {
            return new ArrayList<>();
        }
        return Files.list(DIRECTORY)
                .map(p -> StringUtils.removeEnd(p.getFileName().toString(), FILE_EXTENSION))
                .toList();
    }

    public Path getBoardTypePath(String name) {
        return DIRECTORY.resolve(name + FILE_EXTENSION);
    }

    public JSONBoardType readBoardType(String name) throws IOException {
        Gson gson = new Gson();
        return gson.fromJson(Files.readString(getBoardTypePath(name)), JSONBoardType.class);
    }

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

    public boolean boardTypeExists(String name) {
        return Files.exists(getBoardTypePath(name));
    }

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
