package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.json.JSONBoardType;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.type.BoardTypeIO;
import me.marin.lockout.type.BoardTypeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.awt.*;
import java.io.IOException;
import java.util.*;


/**
 * Screen for creating custom BoardTypes.
 * Allows users to select/deselect individual goals to exclude from board generation.
 */
public class BoardTypeCreatorScreen extends Screen {

    private EditBox nameTextField;
    private EditBox searchTextField;
    private Button saveButton;
    private Button cancelButton;
    private StringWidget errorTextWidget;
    private StringWidget statusTextWidget;
    private BoardTypeGoalListWidget goalListWidget;

    private final Set<String> excludedGoals = new HashSet<>();
    private final String editingBoardTypeName;
    private final boolean isEditMode;

    public BoardTypeCreatorScreen() {
        super(Component.literal("Create Custom BoardType"));
        this.editingBoardTypeName = null;
        this.isEditMode = false;
    }
    
    public BoardTypeCreatorScreen(JSONBoardType existingBoardType) {
        super(Component.literal("Edit BoardType: " + existingBoardType.name));
        this.editingBoardTypeName = existingBoardType.name;
        this.isEditMode = true;
        if (existingBoardType.excludedGoals != null) {
            this.excludedGoals.addAll(existingBoardType.excludedGoals);
        }
    }

    @Override
    protected void init() {
        super.init();
        Font textRenderer = Minecraft.getInstance().font;

        int centerX = width / 2;

        StringWidget titleWidget = new StringWidget(this.title.copy().withStyle(ChatFormatting.BOLD), font);
        titleWidget.setPosition(centerX - titleWidget.getWidth() / 2, 10);
        this.addRenderableWidget(titleWidget);

        // Name input field
        int nameFieldWidth = 200;
        nameTextField = new EditBox(font, centerX - nameFieldWidth / 2, 30, nameFieldWidth, 20, Component.empty());
        nameTextField.setMaxLength(50);
        nameTextField.setSuggestion("BoardType Name");
        if (isEditMode) {
            nameTextField.setValue(editingBoardTypeName);
        }
        nameTextField.setResponder(text -> nameTextField.setSuggestion(text.isEmpty() ? "BoardType Name" : null));
        this.addRenderableWidget(nameTextField);

        // Search field
        int searchFieldWidth = 300;
        searchTextField = new EditBox(font, centerX - searchFieldWidth / 2, 60, searchFieldWidth, 20, Component.empty());
        searchTextField.setMaxLength(100);
        searchTextField.setSuggestion("Search goals...");
        searchTextField.setResponder(text -> {
            searchTextField.setSuggestion(text.isEmpty() ? "Search goals..." : null);
            if (goalListWidget != null) {
                goalListWidget.updateSearch(text);
            }
        });
        this.addRenderableWidget(searchTextField);

        statusTextWidget = new StringWidget(getStatusText(), font);
        statusTextWidget.setPosition(centerX - statusTextWidget.getWidth() / 2, 85);
        this.addRenderableWidget(statusTextWidget);

        int listWidth = 400;
        int listHeight = height - 180;
        goalListWidget = new BoardTypeGoalListWidget(
            centerX - listWidth / 2,
            105,
            listWidth,
            listHeight,
            Component.empty(),
            this
        );
        this.addRenderableWidget(goalListWidget);

        final int BOTTOM_Y = height - 30;

        String saveButtonText = isEditMode ? "Save Changes" : "Save BoardType";
        saveButton = Button.builder(Component.literal(saveButtonText), (b) -> {
            saveBoardType();
        }).width(120).pos(centerX - 125, BOTTOM_Y).build();
        this.addRenderableWidget(saveButton);

        cancelButton = Button.builder(Component.literal("Cancel"), (b) -> {
            onClose();
        }).width(80).pos(centerX + 10, BOTTOM_Y).build();
        this.addRenderableWidget(cancelButton);
    }

    public void toggleGoalExclusion(String goalId) {
        if (!excludedGoals.remove(goalId)) {
            excludedGoals.add(goalId);
        }
        updateStatusText();
    }

    public boolean isGoalExcluded(String goalId) {
        return excludedGoals.contains(goalId);
    }

    private void updateStatusText() {
        if (statusTextWidget != null) {
            this.removeWidget(statusTextWidget);
        }

        Font textRenderer = Minecraft.getInstance().font;
        statusTextWidget = new StringWidget(getStatusText(), font);
        statusTextWidget.setPosition(width / 2 - statusTextWidget.getWidth() / 2, 85);
        this.addRenderableWidget(statusTextWidget);
    }

    private Component getStatusText() {
        int totalGoals = GoalRegistry.INSTANCE.getRegisteredGoals().size();
        int excludedCount = excludedGoals.size();
        int includedCount = totalGoals - excludedCount;
        
        return Component.literal(String.format("Goals: %d included, %d excluded (of %d total)", 
            includedCount, excludedCount, totalGoals))
            .withStyle(ChatFormatting.GRAY);
    }

    private void saveBoardType() {
        if (errorTextWidget != null) {
            this.removeWidget(errorTextWidget);
            errorTextWidget = null;
        }

        String name = nameTextField.getValue().trim();

        if (name.isEmpty()) {
            showError("Please enter a name for the BoardType");
            return;
        }

        if (name.matches(".*[<>:\"/\\\\|?*].*")) {
            showError("Name contains invalid characters");
            return;
        }
        
        if (!isEditMode || !name.equals(editingBoardTypeName)) {
            if (BoardTypeIO.INSTANCE.boardTypeExists(name)) {
                showError("A BoardType with this name already exists");
                return;
            }
        }

        JSONBoardType boardType = new JSONBoardType();
        boardType.name = name;
        boardType.excludedGoals = new ArrayList<>(excludedGoals);
        boardType.description = String.format("Custom BoardType with %d goals excluded", excludedGoals.size());

        try {
            if (isEditMode && !name.equals(editingBoardTypeName)) {
                BoardTypeIO.INSTANCE.deleteBoardType(editingBoardTypeName);
            }

            BoardTypeIO.INSTANCE.saveBoardType(boardType);
            BoardTypeManager.INSTANCE.clearCache();

            String action = isEditMode ? "Updated" : "Created";
            Minecraft.getInstance().player.sendSystemMessage(
                Component.literal(action + " custom BoardType: ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" (" + excludedGoals.size() + " goals excluded)"))
            );

            onClose();
        } catch (IOException e) {
            Lockout.error(e);
            showError("Failed to save BoardType: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (errorTextWidget != null) {
            this.removeWidget(errorTextWidget);
        }

        Font textRenderer = Minecraft.getInstance().font;
        errorTextWidget = new StringWidget(Component.literal(message).withStyle(ChatFormatting.RED), font);
        int centerX = width / 2;
        errorTextWidget.setPosition(centerX - errorTextWidget.getWidth() / 2, height - 55);
        this.addRenderableWidget(errorTextWidget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
