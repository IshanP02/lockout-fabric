package me.marin.lockout.client.gui;

import me.marin.lockout.LockoutConfig;
import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockoutSettingsScreen extends Screen {

    private final Screen parent;
    private AbstractSliderButton scaleSlider;
    private Button boardPositionButton;
    private KeyMapping editingKeyBinding;
    private final Map<KeyMapping, Button> keybindButtons = new HashMap<>();
    private final Map<KeyMapping, Button> resetButtons = new HashMap<>();
    private final Map<KeyMapping, Integer> keybindRowY = new HashMap<>();
    private KeybindOption[] keybindOptions = new KeybindOption[0];
    private int keyLabelX;

    private static final int ROW_HEIGHT = 24;
    private static final int COLUMN_WIDTH = 150;
    private static final int COLUMN_GAP = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int RESET_BUTTON_WIDTH = 50;

    private record KeybindOption(String label, KeyMapping binding, InputConstants.Key defaultKey) {}

    public LockoutSettingsScreen(Screen parent) {
        super(Component.literal("Lockout Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftX = centerX - COLUMN_WIDTH - COLUMN_GAP / 2;
        int rightX = centerX + COLUMN_GAP / 2;
        int y = this.height / 6 - 12;

        double currentScale = Math.max(0.5, Math.min(2.0, LockoutConfig.getInstance().boardScale));
        double sliderValue = Math.max(0.0, Math.min(1.0, (currentScale - 0.5) / 1.5));

        scaleSlider = new AbstractSliderButton(leftX, y, COLUMN_WIDTH, BUTTON_HEIGHT,
            Component.literal("Board Scale: " + String.format("%.1fx", currentScale)), sliderValue) {
            @Override
            protected void updateMessage() {
                double scale = 0.5 + (this.value * 1.5);
                scale = Math.round(scale * 10.0) / 10.0;
                this.setMessage(Component.literal("Board Scale: " + String.format("%.1fx", scale)));
            }

            @Override
            protected void applyValue() {
                double scale = 0.5 + (this.value * 1.5);
                scale = Math.round(scale * 10.0) / 10.0;
                scale = Math.max(0.5, Math.min(2.0, scale));
                LockoutConfig.getInstance().boardScale = scale;
                LockoutConfig.save();
            }
        };
        scaleSlider.setTooltip(Tooltip.create(Component.literal("Adjust board display size (0.5x - 2.0x)")));
        this.addRenderableWidget(scaleSlider);

        LockoutConfig.BoardPosition currentPosition = LockoutConfig.getInstance().boardPosition;
        String positionText = currentPosition == LockoutConfig.BoardPosition.LEFT ? "Left" : "Right";
        boardPositionButton = Button.builder(Component.literal(positionText), (button) -> {
            LockoutConfig.BoardPosition oldPosition = LockoutConfig.getInstance().boardPosition;
            LockoutConfig.BoardPosition newPosition = oldPosition == LockoutConfig.BoardPosition.LEFT
                ? LockoutConfig.BoardPosition.RIGHT 
                : LockoutConfig.BoardPosition.LEFT;
            LockoutConfig.getInstance().boardPosition = newPosition;
            LockoutConfig.save();
            String newText = newPosition == LockoutConfig.BoardPosition.LEFT ? "Left" : "Right";
            button.setMessage(Component.literal(newText));
        }).pos(rightX, y).width( COLUMN_WIDTH).tooltip(Tooltip.create(Component.literal("Toggle board position between Left and Right"))).build();
        this.addRenderableWidget(boardPositionButton);

        y += ROW_HEIGHT + 8;

        List<KeyMapping> bindings = LockoutClient.getLockoutKeyBindings();
        keybindOptions = new KeybindOption[] {
            new KeybindOption("Open Board", getBinding(bindings, 0), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B)),
            new KeybindOption("Open Pick/Ban List", getBinding(bindings, 1), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_P)),
            new KeybindOption("Toggle Board Visibility", getBinding(bindings, 2), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H)),
            new KeybindOption("Toggle Section View", getBinding(bindings, 3), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O)),
            new KeybindOption("Next Section", getBinding(bindings, 4), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_V)),
            new KeybindOption("Toggle Auto-Cycle Sections", getBinding(bindings, 5), InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M))
        };

        int maxLabelWidth = 0;
        for (KeybindOption option : keybindOptions) {
            if (option.binding() != null) {
                maxLabelWidth = Math.max(maxLabelWidth, this.font.width(option.label()));
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

            Button keybindButton = Button.builder(Component.empty(), button -> {
                editingKeyBinding = option.binding();
                updateKeybindButtonMessages();
            }).pos(keyButtonX, rowY).width( keyButtonWidth).build();

            Button resetButton = Button.builder(Component.literal("Reset"), button -> {
                option.binding().setKey(option.defaultKey());
                if (editingKeyBinding == option.binding()) {
                    editingKeyBinding = null;
                }
                updateKeybindButtonMessages();
            }).pos(resetButtonX, rowY).width( RESET_BUTTON_WIDTH).build();

            keybindButtons.put(option.binding(), keybindButton);
            resetButtons.put(option.binding(), resetButton);
            this.addRenderableWidget(keybindButton);
            this.addRenderableWidget(resetButton);
        }

        updateKeybindButtonMessages();

        int doneY = y + keybindOptions.length * ROW_HEIGHT + 12;
        Button doneButton = Button.builder(Component.literal("Done"), (button) -> {
            this.onClose();
        }).pos(centerX - 100, doneY).width( 200).build();
        this.addRenderableWidget(doneButton);
    }

    private static KeyMapping getBinding(List<KeyMapping> bindings, int index) {
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

            Button widget = keybindButtons.get(option.binding());
            if (widget == null) {
                continue;
            }

            String message = option.binding().getTranslatedKeyMessage().getString();
            if (editingKeyBinding == option.binding()) {
                message = "> " + message + " <";
            }
            widget.setMessage(Component.literal(message));

            Button resetButton = resetButtons.get(option.binding());
            if (resetButton != null) {
                resetButton.active = !isBoundTo(option.binding(), option.defaultKey());
            }
        }
    }

    private static boolean isBoundTo(KeyMapping binding, InputConstants.Key key) {
        return binding.getTranslatedKeyMessage().getString().equals(key.getDisplayName().getString());
    }

    private void setBinding(InputConstants.Key key) {
        if (editingKeyBinding == null) {
            return;
        }

        editingKeyBinding.setKey(key);
        editingKeyBinding = null;
        updateKeybindButtonMessages();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (editingKeyBinding != null) {
            setBinding(InputConstants.getKey(input));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean consumed) {
        if (editingKeyBinding != null) {
            setBinding(InputConstants.Type.MOUSE.getOrCreate(click.button()));
            return true;
        }
        return super.mouseClicked(click, consumed);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Avoid calling renderBackground here because this screen can open from another blurred screen.
        context.fill(0, 0, this.width, this.height, 0x60_00_00_00);
        context.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        super.extractRenderState(context, mouseX, mouseY, delta);

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

            Button keyButton = keybindButtons.get(option.binding());
            if (keyButton == null) {
                continue;
            }

            context.text(this.font, option.label(), sharedLabelX, rowY + 6, 0xFFFFFFFF, true);
        }

        if (editingKeyBinding != null) {
            context.centeredText(this.font, Component.literal("Press a key or mouse button"), this.width / 2, this.height - 30, 0xFFFFFF55);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
