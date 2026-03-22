package me.marin.lockout.client.gui;

import me.marin.lockout.network.OpenBlackoutSetupPayload;
import me.marin.lockout.network.SubmitBlackoutSetupPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BlackoutSetupScreen extends Screen {

    private static final int[] BOARD_SIZE_OPTIONS = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final int[] START_TIMER_OPTIONS = {0, 3, 5, 10, 15, 20, 30, 60};

    private final List<String> boardTypes;

    private int boardCount = 0;
    private int boardSize = 0;
    private int selectedBoardTypeIndex = 0;
    private int startTimeCount = 0;
    private int startTimeSize = 0;

    private ButtonWidget boardSizeValueButton;
    private ButtonWidget boardTypeButton;
    private ButtonWidget startTimeButton;

    public BlackoutSetupScreen(OpenBlackoutSetupPayload payload) {
        super(Text.literal("Blackout Setup"));

        this.boardTypes = new ArrayList<>();
        this.boardTypes.add("Default");
        for (String type : payload.availableBoardTypes()) {
            if (!"Default".equalsIgnoreCase(type) && !this.boardTypes.contains(type)) {
                this.boardTypes.add(type);
            }
        }

        this.boardCount = findIndex(BOARD_SIZE_OPTIONS, payload.boardSize());
        this.boardSize = BOARD_SIZE_OPTIONS[this.boardCount];
        this.startTimeCount = findIndex(START_TIMER_OPTIONS, payload.startTimerSeconds());
        this.startTimeSize = START_TIMER_OPTIONS[this.startTimeCount];

        String selected = payload.selectedBoardType();
        if (selected != null && !selected.isBlank()) {
            for (int i = 0; i < this.boardTypes.size(); i++) {
                if (this.boardTypes.get(i).equalsIgnoreCase(selected)) {
                    this.selectedBoardTypeIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        boardSizeValueButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            boardCount++;
            boardSize = BOARD_SIZE_OPTIONS[boardCount % BOARD_SIZE_OPTIONS.length];
            updateBoardSizeButton();
        }).dimensions(centerX + 55, y, 100, 20).build());
        updateBoardSizeButton();

        boardTypeButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            selectedBoardTypeIndex = (selectedBoardTypeIndex + 1) % boardTypes.size();
            updateBoardTypeButton();
        }).dimensions(centerX - 155, y, 180, 20).build());
        updateBoardTypeButton();

        y += 28;

        startTimeButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            startTimeCount++;
            startTimeSize = START_TIMER_OPTIONS[startTimeCount % START_TIMER_OPTIONS.length];
            updateStartTimeButton();
        }).dimensions(centerX - 155, y, 180, 20).build());
        updateStartTimeButton();

        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Start"), button -> {
            String selectedType = boardTypes.get(selectedBoardTypeIndex);
            if ("Default".equals(selectedType)) {
                selectedType = "";
            }

            ClientPlayNetworking.send(new SubmitBlackoutSetupPayload(boardSize, startTimeSize, selectedType));
            this.close();
        }).dimensions(centerX - 155, y, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.close()).dimensions(centerX + 5, y, 150, 20).build());
    }

    private static int findIndex(int[] options, int value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private void updateBoardSizeButton() {
        if (boardSizeValueButton != null) {
            boardSizeValueButton.setMessage(Text.literal("Size: " + boardSize + "x" + boardSize));
        }
    }

    private void updateBoardTypeButton() {
        if (boardTypeButton != null) {
            String name = boardTypes.get(selectedBoardTypeIndex);
            boardTypeButton.setMessage(Text.literal("Board Type: " + name));
        }
    }

        private void updateStartTimeButton() {
        if (startTimeButton != null) {
            String name = startTimeSize + " seconds";
            startTimeButton.setMessage(Text.literal("Start Timer: " + name));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x60_00_00_00);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
