package me.marin.lockout.client.gui;

import me.marin.lockout.LockoutConfig;
import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockoutSettingsScreen extends Screen {

    private final Screen parent;
    private SliderWidget scaleSlider;
    private ButtonWidget boardPositionButton;
    private KeyBinding editingKeyBinding;
    private final Map<KeyBinding, ButtonWidget> keybindButtons = new HashMap<>();
    private final Map<KeyBinding, ButtonWidget> resetButtons = new HashMap<>();
    private final Map<KeyBinding, Integer> keybindRowY = new HashMap<>();
    private KeybindOption[] keybindOptions = new KeybindOption[0];
    private int keyLabelX;

    private static final int ROW_HEIGHT = 24;
    private static final int COLUMN_WIDTH = 150;
    private static final int COLUMN_GAP = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int RESET_BUTTON_WIDTH = 50;

    private record KeybindOption(String label, KeyBinding binding, InputUtil.Key defaultKey) {}

    public LockoutSettingsScreen(Screen parent) {
        super(Text.literal("Lockout Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftX = centerX - COLUMN_WIDTH - COLUMN_GAP / 2;
        int rightX = centerX + COLUMN_GAP / 2;
        int y = this.height / 6 - 12;

        double currentScale = LockoutConfig.getInstance().boardScale;
        double sliderValue = (currentScale - 0.5) / 1.5;

        scaleSlider = new SliderWidget(leftX, y, COLUMN_WIDTH, BUTTON_HEIGHT,
            Text.literal("Board Scale: " + String.format("%.1fx", currentScale)), sliderValue) {
            @Override
            protected void updateMessage() {
                double scale = 0.5 + (this.value * 1.5);
                scale = Math.round(scale * 10.0) / 10.0;
                this.setMessage(Text.literal("Board Scale: " + String.format("%.1fx", scale)));
            }

            @Override
            protected void applyValue() {
                double scale = 0.5 + (this.value * 1.5);
                scale = Math.round(scale * 10.0) / 10.0;
                LockoutConfig.getInstance().boardScale = scale;
                LockoutConfig.save();
            }
        };
        scaleSlider.setTooltip(Tooltip.of(Text.literal("Adjust board display size (0.5x - 2.0x)")));
        this.addDrawableChild(scaleSlider);

        LockoutConfig.BoardPosition currentPosition = LockoutConfig.getInstance().boardPosition;
        String positionText = currentPosition == LockoutConfig.BoardPosition.LEFT ? "Left" : "Right";
        boardPositionButton = ButtonWidget.builder(Text.literal(positionText), (button) -> {
            LockoutConfig.BoardPosition oldPosition = LockoutConfig.getInstance().boardPosition;
            LockoutConfig.BoardPosition newPosition = oldPosition == LockoutConfig.BoardPosition.LEFT
                ? LockoutConfig.BoardPosition.RIGHT 
                : LockoutConfig.BoardPosition.LEFT;
            LockoutConfig.getInstance().boardPosition = newPosition;
            LockoutConfig.save();
            String newText = newPosition == LockoutConfig.BoardPosition.LEFT ? "Left" : "Right";
            button.setMessage(Text.literal(newText));
        }).dimensions(rightX, y, COLUMN_WIDTH, BUTTON_HEIGHT).tooltip(Tooltip.of(Text.literal("Toggle board position between Left and Right"))).build();
        this.addDrawableChild(boardPositionButton);

        y += ROW_HEIGHT + 8;

        List<KeyBinding> bindings = LockoutClient.getLockoutKeyBindings();
        keybindOptions = new KeybindOption[] {
            new KeybindOption("Open Board", getBinding(bindings, 0), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_B)),
            new KeybindOption("Open Pick/Ban List", getBinding(bindings, 1), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_P)),
            new KeybindOption("Toggle Board Visibility", getBinding(bindings, 2), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_H)),
            new KeybindOption("Toggle Section View", getBinding(bindings, 3), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_O)),
            new KeybindOption("Next Section", getBinding(bindings, 4), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_V)),
            new KeybindOption("Toggle Auto-Cycle Sections", getBinding(bindings, 5), InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_M))
        };

        int maxLabelWidth = 0;
        for (KeybindOption option : keybindOptions) {
            if (option.binding() != null) {
                maxLabelWidth = Math.max(maxLabelWidth, this.textRenderer.getWidth(option.label()));
            }
        }

        int rightEdge = rightX + COLUMN_WIDTH;
        keyLabelX = leftX;
        int keyButtonX = keyLabelX + maxLabelWidth + 10;
        int resetButtonX = rightEdge - RESET_BUTTON_WIDTH;
        int keyButtonWidth = Math.max(70, resetButtonX - 6 - keyButtonX);

        for (int i = 0; i < keybindOptions.length; i++) {
            KeybindOption option = keybindOptions[i];
            if (option.binding() == null) {
                continue;
            }

            int rowY = y + i * ROW_HEIGHT;
            keybindRowY.put(option.binding(), rowY);

            ButtonWidget keybindButton = ButtonWidget.builder(Text.empty(), button -> {
                editingKeyBinding = option.binding();
                updateKeybindButtonMessages();
            }).dimensions(keyButtonX, rowY, keyButtonWidth, BUTTON_HEIGHT).build();

            ButtonWidget resetButton = ButtonWidget.builder(Text.literal("Reset"), button -> {
                option.binding().setBoundKey(option.defaultKey());
                KeyBinding.updateKeysByCode();
                if (editingKeyBinding == option.binding()) {
                    editingKeyBinding = null;
                }
                updateKeybindButtonMessages();
            }).dimensions(resetButtonX, rowY, RESET_BUTTON_WIDTH, BUTTON_HEIGHT).build();

            keybindButtons.put(option.binding(), keybindButton);
            resetButtons.put(option.binding(), resetButton);
            this.addDrawableChild(keybindButton);
            this.addDrawableChild(resetButton);
        }

        updateKeybindButtonMessages();

        int doneY = y + keybindOptions.length * ROW_HEIGHT + 12;
        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), (button) -> {
            this.close();
        }).dimensions(centerX - 100, doneY, 200, BUTTON_HEIGHT).build();
        this.addDrawableChild(doneButton);
    }

    private static KeyBinding getBinding(List<KeyBinding> bindings, int index) {
        if (index < 0 || index >= bindings.size()) {
            return null;
        }
        return bindings.get(index);
    }

    private void updateKeybindButtonMessages() {
        for (KeybindOption option : keybindOptions) {
            if (option.binding() == null) {
                continue;
            }

            ButtonWidget widget = keybindButtons.get(option.binding());
            if (widget == null) {
                continue;
            }

            String message = option.binding().getBoundKeyLocalizedText().getString();
            if (editingKeyBinding == option.binding()) {
                message = "> " + message + " <";
            }
            widget.setMessage(Text.literal(message));

            ButtonWidget resetButton = resetButtons.get(option.binding());
            if (resetButton != null) {
                resetButton.active = !isBoundTo(option.binding(), option.defaultKey());
            }
        }
    }

    private static boolean isBoundTo(KeyBinding binding, InputUtil.Key key) {
        return binding.getBoundKeyLocalizedText().getString().equals(key.getLocalizedText().getString());
    }

    private void setBinding(InputUtil.Key key) {
        if (editingKeyBinding == null) {
            return;
        }

        editingKeyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
        editingKeyBinding = null;
        updateKeybindButtonMessages();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (editingKeyBinding != null) {
            setBinding(InputUtil.fromKeyCode(input));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean consumed) {
        if (editingKeyBinding != null) {
            setBinding(InputUtil.Type.MOUSE.createFromCode(click.button()));
            return true;
        }
        return super.mouseClicked(click, consumed);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Avoid calling renderBackground here because this screen can open from another blurred screen.
        context.fill(0, 0, this.width, this.height, 0x60_00_00_00);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int sharedLabelX = keyLabelX;

        for (KeybindOption option : keybindOptions) {
            if (option.binding() == null) {
                continue;
            }
            Integer rowY = keybindRowY.get(option.binding());
            if (rowY == null) {
                continue;
            }

            ButtonWidget keyButton = keybindButtons.get(option.binding());
            if (keyButton == null) {
                continue;
            }

            context.drawText(this.textRenderer, option.label(), sharedLabelX, rowY + 6, 0xFFFFFFFF, true);
        }

        if (editingKeyBinding != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press a key or mouse button"), this.width / 2, this.height - 30, 0xFFFFFF55);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
