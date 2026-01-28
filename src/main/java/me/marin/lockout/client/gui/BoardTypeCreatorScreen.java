package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.json.JSONBoardType;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.type.BoardTypeIO;
import me.marin.lockout.type.BoardTypeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.io.IOException;
import java.util.*;


/**
 * Screen for creating custom BoardTypes.
 * Allows users to select/deselect individual goals to exclude from board generation.
 */
public class BoardTypeCreatorScreen extends Screen {

    private TextFieldWidget nameTextField;
    private TextFieldWidget searchTextField;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private TextWidget errorTextWidget;
    private TextWidget statusTextWidget;
    private BoardTypeGoalListWidget goalListWidget;

    private final Set<String> excludedGoals = new HashSet<>();
    private final String editingBoardTypeName;
    private final boolean isEditMode;

    public BoardTypeCreatorScreen() {
        super(Text.literal("Create Custom BoardType"));
        this.editingBoardTypeName = null;
        this.isEditMode = false;
    }
    
    public BoardTypeCreatorScreen(JSONBoardType existingBoardType) {
        super(Text.literal("Edit BoardType: " + existingBoardType.name));
        this.editingBoardTypeName = existingBoardType.name;
        this.isEditMode = true;
        if (existingBoardType.excludedGoals != null) {
            this.excludedGoals.addAll(existingBoardType.excludedGoals);
        }
    }

    @Override
    protected void init() {
        super.init();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int centerX = width / 2;

        TextWidget titleWidget = new TextWidget(this.title.copy().formatted(Formatting.BOLD), textRenderer);
        titleWidget.setPosition(centerX - titleWidget.getWidth() / 2, 10);
        this.addDrawableChild(titleWidget);

        // Name input field
        int nameFieldWidth = 200;
        nameTextField = new TextFieldWidget(textRenderer, centerX - nameFieldWidth / 2, 30, nameFieldWidth, 20, Text.empty());
        nameTextField.setMaxLength(50);
        nameTextField.setSuggestion("BoardType Name");
        if (isEditMode) {
            nameTextField.setText(editingBoardTypeName);
        }
        nameTextField.setChangedListener(text -> nameTextField.setSuggestion(text.isEmpty() ? "BoardType Name" : null));
        this.addDrawableChild(nameTextField);

        // Search field
        int searchFieldWidth = 300;
        searchTextField = new TextFieldWidget(textRenderer, centerX - searchFieldWidth / 2, 60, searchFieldWidth, 20, Text.empty());
        searchTextField.setMaxLength(100);
        searchTextField.setSuggestion("Search goals...");
        searchTextField.setChangedListener(text -> {
            searchTextField.setSuggestion(text.isEmpty() ? "Search goals..." : null);
            if (goalListWidget != null) {
                goalListWidget.updateSearch(text);
            }
        });
        this.addDrawableChild(searchTextField);

        statusTextWidget = new TextWidget(getStatusText(), textRenderer);
        statusTextWidget.setPosition(centerX - statusTextWidget.getWidth() / 2, 85);
        this.addDrawableChild(statusTextWidget);

        int listWidth = 400;
        int listHeight = height - 180;
        goalListWidget = new BoardTypeGoalListWidget(
            centerX - listWidth / 2,
            105,
            listWidth,
            listHeight,
            Text.empty(),
            this
        );
        this.addDrawableChild(goalListWidget);

        final int BOTTOM_Y = height - 30;

        String saveButtonText = isEditMode ? "Save Changes" : "Save BoardType";
        saveButton = ButtonWidget.builder(Text.of(saveButtonText), (b) -> {
            saveBoardType();
        }).width(120).position(centerX - 125, BOTTOM_Y).build();
        this.addDrawableChild(saveButton);

        cancelButton = ButtonWidget.builder(Text.of("Cancel"), (b) -> {
            close();
        }).width(80).position(centerX + 10, BOTTOM_Y).build();
        this.addDrawableChild(cancelButton);
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
            this.remove(statusTextWidget);
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        statusTextWidget = new TextWidget(getStatusText(), textRenderer);
        statusTextWidget.setPosition(width / 2 - statusTextWidget.getWidth() / 2, 85);
        this.addDrawableChild(statusTextWidget);
    }

    private Text getStatusText() {
        int totalGoals = GoalRegistry.INSTANCE.getRegisteredGoals().size();
        int excludedCount = excludedGoals.size();
        int includedCount = totalGoals - excludedCount;
        
        return Text.literal(String.format("Goals: %d included, %d excluded (of %d total)", 
            includedCount, excludedCount, totalGoals))
            .formatted(Formatting.GRAY);
    }

    private void saveBoardType() {
        if (errorTextWidget != null) {
            this.remove(errorTextWidget);
            errorTextWidget = null;
        }

        String name = nameTextField.getText().trim();

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
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal(action + " custom BoardType: ").formatted(Formatting.GREEN)
                    .append(Text.literal(name).formatted(Formatting.YELLOW))
                    .append(Text.literal(" (" + excludedGoals.size() + " goals excluded)")),
                false
            );

            close();
        } catch (IOException e) {
            Lockout.error(e);
            showError("Failed to save BoardType: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (errorTextWidget != null) {
            this.remove(errorTextWidget);
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        errorTextWidget = new TextWidget(Text.literal(message).formatted(Formatting.RED), textRenderer);
        int centerX = width / 2;
        errorTextWidget.setPosition(centerX - errorTextWidget.getWidth() / 2, height - 55);
        this.addDrawableChild(errorTextWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
