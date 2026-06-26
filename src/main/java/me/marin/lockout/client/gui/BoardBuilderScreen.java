package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.Utility;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.json.JSONBoard;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static me.marin.lockout.Constants.*;

public class BoardBuilderScreen extends Screen {

    public static int CENTER_OFFSET = 0;
    public static boolean displaySearch = false;
    public static boolean displayEditData = false;

    private EditBox titleTextField;
    private Button saveButton;
    private StringWidget saveErrorTextWidget;
    private Button clearBoardButton;
    private Button closeButton;
    private Button closeSearchButton;
    private Button increaseSizeButton;
    private Button decreaseSizeButton;
    private EditBox searchTextField;
    private BoardBuilderSearchWidget boardBuilderSearchWidget;
    private Button saveDataButton;
    private Button closeEditDataButton;
    private StringWidget editDataErrorTextWidget;


    public BoardBuilderScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();
        Font textRenderer = Minecraft.getInstance().font;

        int centerX = width / 2;
        int centerY = height / 2;

        int boardHalfSize = GUI_PADDING + (BoardBuilderData.INSTANCE.size() * GUI_SLOT_SIZE) / 2;

        titleTextField = new EditBox(font, centerX - 60 - CENTER_OFFSET, centerY - boardHalfSize - 18 - 8, 120, 18, Component.empty());
        titleTextField.setResponder(BoardBuilderData.INSTANCE::setTitle);
        titleTextField.setValue(BoardBuilderData.INSTANCE.getTitle());
        this.addRenderableWidget(titleTextField);

        final int BOTTOM_BUTTONS_Y = height - 30;

        saveButton = Button.builder(Component.literal("Save Board"), (b) -> {
            saveGoals(10, height - 45);
        }).width(85).pos(10, BOTTOM_BUTTONS_Y).build();
        this.addRenderableWidget(saveButton);

        closeButton = Button.builder(Component.literal("Close"), (b) -> {
            onClose();
        }).width(50).pos(width - 50 - 10, BOTTOM_BUTTONS_Y).build();
        this.addRenderableWidget(closeButton);

        clearBoardButton = Button.builder(Component.literal("Clear Board"), (b) -> {
            BoardBuilderData.INSTANCE.clear();
            closeEditData();
            closeSearch();
        }).width(85).pos(closeButton.getX() - 85 - 10, BOTTOM_BUTTONS_Y).build();
        this.addRenderableWidget(clearBoardButton);

        increaseSizeButton = Button.builder(Component.literal("+"), b -> {
            BoardBuilderData.INSTANCE.incrementSize();
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.literal("Increase board size"))).width(20).pos(centerX + boardHalfSize - CENTER_OFFSET + 8, centerY - 10).build();
        increaseSizeButton.active = BoardBuilderData.INSTANCE.size() != MAX_BOARD_SIZE;
        this.addRenderableWidget(increaseSizeButton);

        decreaseSizeButton = Button.builder(Component.literal("-"), b -> {
            BoardBuilderData.INSTANCE.decrementSize();
            rebuildWidgets();
        }).tooltip(Tooltip.create(Component.literal("Decrease board size"))).width(20).pos(centerX - boardHalfSize - CENTER_OFFSET - 20 - 8, centerY - 10).build();
        decreaseSizeButton.active = BoardBuilderData.INSTANCE.size() != MIN_BOARD_SIZE;
        this.addRenderableWidget(decreaseSizeButton);

        if (displaySearch) {
            double scrollY = boardBuilderSearchWidget == null ? 0 : boardBuilderSearchWidget.getScrollY();
            boardBuilderSearchWidget = new BoardBuilderSearchWidget(
                    centerX + boardHalfSize + 35 - CENTER_OFFSET,
                    40,
                    width / 2 - 125 + CENTER_OFFSET,
                    height - 40 * 2, Component.empty(), false, false, BoardBuilderData.INSTANCE.getSearch());
            boardBuilderSearchWidget.setScrollY(scrollY);
            this.addRenderableWidget(boardBuilderSearchWidget);

            closeSearchButton = Button.builder(Component.literal("<"), (b) -> {
                closeSearch();
            }).tooltip(Tooltip.create(Component.literal("Close search"))).width(20).pos(boardBuilderSearchWidget.getX(), boardBuilderSearchWidget.getY() - 21).build();
            this.addRenderableWidget(closeSearchButton);

            searchTextField = new EditBox(font, closeSearchButton.getX() + closeSearchButton.getWidth() + 1 + 5, closeSearchButton.getY() + 1, boardBuilderSearchWidget.getWidth() - closeSearchButton.getWidth() - 2 - 5, 18, Component.empty());
            searchTextField.setResponder(s -> {
                BoardBuilderData.INSTANCE.setSearch(s);
                boardBuilderSearchWidget.searchUpdated(s);
            });
            if (!BoardBuilderData.INSTANCE.getSearch().isEmpty()) {
                searchTextField.setValue(BoardBuilderData.INSTANCE.getSearch());
            }
            this.addRenderableWidget(searchTextField);
        }
        if (displayEditData) {
            Goal goal = BoardBuilderData.INSTANCE.getModifyingGoal();
            var generators = GoalRegistry.INSTANCE.getDataGenerator(goal.getId()).get().getGenerators();
            List<String> dataList = new ArrayList<>(List.of(goal.getData().split(GoalDataConstants.DATA_SEPARATOR)));
            int x = centerX + 100 - CENTER_OFFSET;
            int y = centerY - (18 + generators.size() * 45) / 2;

            for (int i = 0; i < generators.size(); i++) {
                final int idx = i;
                GoalDataGenerator.Generator<?> generator = generators.get(i);
                StringWidget StringWidget = new StringWidget(Component.literal(generator.getGeneratorName()), font);
                StringWidget.setPosition(x, y);
                this.addRenderableWidget(StringWidget);

                y += 15;

                EditBox textFieldWidget = new EditBox(font, x, y, 150, 18, Component.empty());
                textFieldWidget.setValue(dataList.get(idx));
                textFieldWidget.setResponder(s -> {
                    dataList.set(idx, s);
                });
                this.addRenderableWidget(textFieldWidget);

                y += 30;
            }

            closeEditDataButton = Button.builder(Component.literal("<"), (b) -> {
                closeEditData();
            }).tooltip(Tooltip.create(Component.literal("Close 'Edit Data'"))).width(20).pos(x, y).build();
            this.addRenderableWidget(closeEditDataButton);

            int errorY = y + 25;
            saveDataButton = Button.builder(Component.literal("Save"), (b) -> {
                StringBuilder sb = new StringBuilder();
                boolean isOk = true;
                String wrongDataGenerator = null;
                for (int i = 0; i < generators.size(); i++) {
                    if (i > 0) sb.append(GoalDataConstants.DATA_SEPARATOR);
                    GoalDataGenerator.Generator<?> generator = generators.get(i);
                    if (!generator.verify(dataList.get(i))) {
                        isOk = false;
                        wrongDataGenerator = generator.getGeneratorName();
                        break;
                    }
                    sb.append(dataList.get(i));
                }
                if (editDataErrorTextWidget != null) {
                    this.removeWidget(editDataErrorTextWidget);
                    this.editDataErrorTextWidget = null;
                }
                if (!isOk) {
                    String s = "Invalid '" + wrongDataGenerator + "'.";
                    editDataErrorTextWidget = new StringWidget(Component.literal(s).withStyle(ChatFormatting.RED), font);
                    editDataErrorTextWidget.setPosition(x + 75 - font.width(s) / 2, errorY);
                    this.addRenderableWidget(editDataErrorTextWidget);
                    return;
                }
                BoardBuilderData.INSTANCE.setGoal(GoalRegistry.INSTANCE.newGoal(goal.getId(), sb.toString()));
                closeEditData();
            }).width(50).pos(closeEditDataButton.getX() + closeEditDataButton.getWidth() + 5, closeEditDataButton.getY()).build();
            this.addRenderableWidget(saveDataButton);
        }

    }

    private void saveGoals(int errorX, int errorY) {
        List<Goal> goals = BoardBuilderData.INSTANCE.getGoals();
        JSONBoard jsonBoard = new JSONBoard();
        List<JSONBoard.JSONGoal> goalList = new ArrayList<>();
        for (Goal goal : goals) {
            if (goal == null) {
                showError("The board is not full.", errorX, errorY);
                return;
            }

            JSONBoard.JSONGoal jsonGoal = new JSONBoard.JSONGoal();
            jsonGoal.id = goal.getId();
            if (goal.hasData()) {
                jsonGoal.data = goal.getData();
            }
            goalList.add(jsonGoal);
        }
        jsonBoard.goals = goalList;

        if (new HashSet<>(goals).size() < goals.size()) {
            showError("Some goals are duplicated, fix and try again.", errorX, errorY);
            return;
        }

        String boardName = BoardBuilderData.INSTANCE.getTitle().trim();
        if (boardName.isBlank()) {
            boardName = "Custom Board";
        }

        try {
            boardName = BoardBuilderIO.INSTANCE.getSuitableName(boardName);
            BoardBuilderIO.INSTANCE.saveBoard(boardName, jsonBoard);
        } catch (IOException e) {
            showError("Error while saving board. Check logs.", errorX, errorY);
            Lockout.error(e);
            return;
        }

        String finalBoardName = boardName;
        // TODO: Fix board builder
        Component openBoardFile = Component.literal("[Open file]").withStyle(style ->
                style.withClickEvent(new ClickEvent.OpenFile(BoardBuilderIO.INSTANCE.getBoardPath(finalBoardName).toFile().getAbsolutePath()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("MouseButtonEvent to open board file.")))
                        .applyFormat(ChatFormatting.WHITE)
        );
        Component openBoardsDirectory = Component.literal("[View all boards]").withStyle(style ->
                style.withClickEvent(new ClickEvent.OpenFile(BoardBuilderIO.DIRECTORY.toFile().getAbsolutePath()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("MouseButtonEvent to open boards directory.")))
                        .applyFormat(ChatFormatting.WHITE)
        );
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Saved custom board as " + boardName + BoardBuilderIO.FILE_EXTENSION + "!\n").withStyle(ChatFormatting.GREEN).append(openBoardFile).append(" ").append(openBoardsDirectory));
        onClose();
    }

    private void showError(String message, int x, int y) {
        saveErrorTextWidget = new StringWidget(Component.literal(message).withStyle(ChatFormatting.RED), this.font);
        saveErrorTextWidget.setPosition(x, y);
        this.addRenderableWidget(saveErrorTextWidget);
    }

    @Override
    public void onClose() {
        closeSearch();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw the board first so child widgets (search, buttons) render on top
        drawCenterBoard(context, mouseX, mouseY);
        //this.extractBackground(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);

        titleTextField.setSuggestion(titleTextField.getValue().isEmpty() ? "Board Name" : null);
        if (displaySearch) {
            searchTextField.setSuggestion(searchTextField.getValue().isEmpty() ? "Search goals.." : null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
        Optional<Integer> hoveredIdx = Utility.getBoardHoveredIndex(BoardBuilderData.INSTANCE.size(), width, height, (int) click.x(), (int) click.y(), false);
        if ((click.button() == 0 || click.button() == 1) && hoveredIdx.isPresent()) {
            Goal goal = BoardBuilderData.INSTANCE.getGoals().get(hoveredIdx.get());
            if (click.button() == 1 && goal != null && goal.hasData()) {
                openEditData(hoveredIdx.get());
            } else {
                openSearch(hoveredIdx.get());
            }
            return true;
        } else {
            return super.mouseClicked(click, consumed);
        }
    }

    public void openSearch(int hoveredIdx) {
        displayEditData = false;
        displaySearch = true;

        BoardBuilderData.INSTANCE.setModifyingIdx(hoveredIdx);
        CENTER_OFFSET = 100;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        rebuildWidgets();
    }

    public void closeSearch() {
        displaySearch = false;

        BoardBuilderData.INSTANCE.setModifyingIdx(null);
        CENTER_OFFSET = 0;

        this.boardBuilderSearchWidget = null;
        this.closeSearchButton = null;
        this.searchTextField = null;

        rebuildWidgets();
    }

    public void openEditData(int hoveredIdx) {
        displaySearch = false;
        displayEditData = true;

        BoardBuilderData.INSTANCE.setModifyingIdx(hoveredIdx);
        CENTER_OFFSET = 50;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        rebuildWidgets();
    }

    public void closeEditData() {
        displayEditData = false;

        BoardBuilderData.INSTANCE.setModifyingIdx(null);
        CENTER_OFFSET = 0;

        this.saveDataButton = null;
        this.closeEditDataButton = null;
        this.editDataErrorTextWidget = null;

        rebuildWidgets();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public void drawCenterBoard(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Font textRenderer = Minecraft.getInstance().font;

        int size = BoardBuilderData.INSTANCE.size();

        int boardWidth = 2 * GUI_CENTER_PADDING + size * GUI_CENTER_SLOT_SIZE;
        int boardHeight = 2 * GUI_CENTER_PADDING + size * GUI_CENTER_SLOT_SIZE;
        int x = width / 2 - boardWidth / 2 - CENTER_OFFSET;
        int y = height / 2 - boardHeight / 2;

        context.blitSprite(RenderPipelines.GUI_TEXTURED, GUI_CENTER_IDENTIFIER, x, y, boardWidth, boardHeight);

        x += GUI_CENTER_PADDING + 1;
        y += GUI_CENTER_PADDING + 1;
        final int startX = x;

        Optional<Integer> hoveredIdx = Utility.getBoardHoveredIndex(BoardBuilderData.INSTANCE.size(), width, height, mouseX, mouseY, false);
        Integer editingIdx = BoardBuilderData.INSTANCE.getModifyingIdx();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int idx = j + size * i;
                Goal goal = BoardBuilderData.INSTANCE.getGoals().get(idx);
                if (goal != null) {
                    goal.render(context, font, x, y);
                }

                if (hoveredIdx.isPresent() && hoveredIdx.get() == idx) {
                    context.fill(x, y, x + 16, y + 16, GUI_CENTER_HOVERED_COLOR);
                }
                if (editingIdx != null && editingIdx == idx) {
                    drawBorder(context, x - 1, y - 1, GUI_SLOT_SIZE, GUI_SLOT_SIZE, Color.RED.getRGB());
                }
                if (hoveredIdx.isPresent() && hoveredIdx.get() == idx) {
                    if (goal != null) {
                        List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                        tooltip.add(Component.literal(goal.getGoalName()));
                        if (goal.hasData()) {
                            tooltip.add(Component.literal("Right-click to edit data.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                        }
                        context.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
                    }
                }

                x += GUI_CENTER_SLOT_SIZE;
            }
            y += GUI_CENTER_SLOT_SIZE;
            x = startX;
        }
    }

    private static void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

}