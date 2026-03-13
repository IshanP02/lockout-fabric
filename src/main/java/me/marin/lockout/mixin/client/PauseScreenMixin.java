package me.marin.lockout.mixin.client;

import me.marin.lockout.Constants;
import me.marin.lockout.client.gui.LockoutSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Lockout Settings" button to the pause menu.
 */
@Mixin(GameMenuScreen.class)
public class PauseScreenMixin extends Screen {

    @Unique
    private static final Identifier LOCK_ICON_TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/gui/sprites/lock.png");
    @Unique
    private static final Text OPTIONS_BUTTON_TEXT = Text.translatable("menu.options");
    @Unique
    private static final int LOCK_BUTTON_SIZE = 20;
    @Unique
    private ButtonWidget lockoutSettingsButton;

    public PauseScreenMixin(Text component) {
        super(component);
    }

    @Inject(
        method = "init",
        at = @At("TAIL")
    )
    private void lockout$addLockoutSettingsButton(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        int buttonX = this.width / 2 - 126;
        int buttonY = this.height / 4 + 72;

        for (Element element : this.children()) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            if (button.getMessage().getString().equals(OPTIONS_BUTTON_TEXT.getString())) {
                buttonX = button.getX() - LOCK_BUTTON_SIZE - 4;
                buttonY = button.getY();
                break;
            }
        }

        lockoutSettingsButton = ButtonWidget.builder(
            Text.empty(),
            button -> {
                client.setScreen(new LockoutSettingsScreen(this));
            }
        )
        .dimensions(buttonX, buttonY, LOCK_BUTTON_SIZE, LOCK_BUTTON_SIZE)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Lockout Settings")))
        .build();

        this.addDrawableChild(lockoutSettingsButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void lockout$renderLockIcon(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (lockoutSettingsButton == null || !lockoutSettingsButton.visible) {
            return;
        }

        int iconSize = 18;
        int iconX = lockoutSettingsButton.getX() + (LOCK_BUTTON_SIZE - iconSize) / 2;
        int iconY = lockoutSettingsButton.getY() + (LOCK_BUTTON_SIZE - iconSize) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOCK_ICON_TEXTURE, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }
}
