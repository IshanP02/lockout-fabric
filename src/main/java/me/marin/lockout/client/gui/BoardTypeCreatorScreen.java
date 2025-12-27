package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.json.JSONBoardType;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.type.BoardType;
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

    // Track which goals are excluded (selected for exclusion)
    private final Set<String> excludedGoals = new HashSet<>();

    public BoardTypeCreatorScreen() {
        super(Text.literal("Create Custom BoardType"));
    }

    @Override
    protected void init() {
        super.init();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int centerX = width / 2;

        // Title
        TextWidget titleWidget = new TextWidget(Text.literal("Create Custom BoardType").formatted(Formatting.BOLD), textRenderer);
        titleWidget.setPosition(centerX - titleWidget.getWidth() / 2, 10);
        this.addDrawableChild(titleWidget);

        // Name input field
        int nameFieldWidth = 200;
        nameTextField = new TextFieldWidget(textRenderer, centerX - nameFieldWidth / 2, 30, nameFieldWidth, 20, Text.empty());
        nameTextField.setMaxLength(50);
        nameTextField.setSuggestion("BoardType Name");
        nameTextField.setChangedListener(text -> {
            nameTextField.setSuggestion(text.isEmpty() ? "BoardType Name" : null);
        });
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

        // Status text (shows count of excluded goals)
        statusTextWidget = new TextWidget(getStatusText(), textRenderer);
        statusTextWidget.setPosition(centerX - statusTextWidget.getWidth() / 2, 85);
        this.addDrawableChild(statusTextWidget);

        // Goal list widget (scrollable)
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

        // Bottom buttons
        final int BOTTOM_Y = height - 30;

        saveButton = ButtonWidget.builder(Text.of("Save BoardType"), (b) -> {
            saveBoardType();
        }).width(120).position(centerX - 125, BOTTOM_Y).build();
        this.addDrawableChild(saveButton);

        cancelButton = ButtonWidget.builder(Text.of("Cancel"), (b) -> {
            close();
        }).width(80).position(centerX + 10, BOTTOM_Y).build();
        this.addDrawableChild(cancelButton);
    }

    /**
     * Toggles a goal's exclusion status
     */
    public void toggleGoalExclusion(String goalId) {
        if (excludedGoals.contains(goalId)) {
            excludedGoals.remove(goalId);
        } else {
            excludedGoals.add(goalId);
        }
        updateStatusText();
    }

    /**
     * Checks if a goal is currently marked for exclusion
     */
    public boolean isGoalExcluded(String goalId) {
        return excludedGoals.contains(goalId);
    }

    /**
     * Updates the status text showing excluded/total goal count
     */
    private void updateStatusText() {
        if (statusTextWidget != null) {
            this.remove(statusTextWidget);
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        statusTextWidget = new TextWidget(getStatusText(), textRenderer);
        statusTextWidget.setPosition(width / 2 - statusTextWidget.getWidth() / 2, 85);
        this.addDrawableChild(statusTextWidget);
    }

    /**
     * Gets the status text for display
     */
    private Text getStatusText() {
        int totalGoals = GoalRegistry.INSTANCE.getRegisteredGoals().size();
        int excludedCount = excludedGoals.size();
        int includedCount = totalGoals - excludedCount;
        
        return Text.literal(String.format("Goals: %d included, %d excluded (of %d total)", 
            includedCount, excludedCount, totalGoals))
            .formatted(Formatting.GRAY);
    }

    /**
     * Saves the BoardType to file
     */
    private void saveBoardType() {
        // Clear previous error
        if (errorTextWidget != null) {
            this.remove(errorTextWidget);
            errorTextWidget = null;
        }

        String name = nameTextField.getText().trim();

        // Validate name
        if (name.isEmpty()) {
            showError("Please enter a name for the BoardType");
            return;
        }

        // Check for invalid characters
        if (name.matches(".*[<>:\"/\\\\|?*].*")) {
            showError("Name contains invalid characters");
            return;
        }

        // Check if name conflicts with built-in types
        try {
            BoardType.valueOf(name.toUpperCase());
            showError("Name conflicts with built-in BoardType: " + name.toUpperCase());
            return;
        } catch (IllegalArgumentException e) {
            // Good, it's not a built-in type
        }

        // Create JSON object
        JSONBoardType boardType = new JSONBoardType();
        boardType.name = name;
        boardType.excludedGoals = new ArrayList<>(excludedGoals);
        boardType.description = String.format("Custom BoardType with %d goals excluded", excludedGoals.size());

        // Save to file
        try {
            // Check if file exists and handle naming
            if (BoardTypeIO.INSTANCE.boardTypeExists(name)) {
                String suitableName = BoardTypeIO.INSTANCE.getSuitableName(name);
                if (!suitableName.equals(name)) {
                    showError("A BoardType with this name already exists. Try: " + suitableName);
                    return;
                }
            }

            BoardTypeIO.INSTANCE.saveBoardType(boardType);
            BoardTypeManager.INSTANCE.clearCache(); // Clear cache so new type is recognized

            // Show success message
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Created custom BoardType: ").formatted(Formatting.GREEN)
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

    /**
     * Shows an error message
     */
    private void showError(String message) {
        if (errorTextWidget != null) {
            this.remove(errorTextWidget);
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        errorTextWidget = new TextWidget(Text.of(message), textRenderer);
        errorTextWidget.setTextColor(Color.RED.getRGB());
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
