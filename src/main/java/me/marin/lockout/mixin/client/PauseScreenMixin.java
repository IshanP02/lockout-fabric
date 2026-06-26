package me.marin.lockout.mixin.client;

import me.marin.lockout.Constants;
import me.marin.lockout.client.gui.LockoutSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Lockout Settings" button to the pause menu.
 */
@Mixin(PauseScreen.class)
public class PauseScreenMixin extends Screen {

    @Unique
    private static final Identifier LOCK_ICON_TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/gui/sprites/lock.png");
    @Unique
    private static final Component OPTIONS_BUTTON_TEXT = Component.translatable("menu.options");
    @Unique
    private static final int LOCK_BUTTON_SIZE = 20;
    @Unique
    private Button lockoutSettingsButton;

    public PauseScreenMixin(Component component) {
        super(component);
    }

    @Inject(
        method = "init",
        at = @At("TAIL")
    )
    private void lockout$addLockoutSettingsButton(CallbackInfo ci) {
        if (!((PauseScreen)(Object)this).showsPauseMenu()) return;

        Minecraft client = Minecraft.getInstance();

        int buttonX = this.width / 2 - 126;
        int buttonY = this.height / 4 + 72;

        for (GuiEventListener element : this.children()) {
            if (!(element instanceof Button button)) {
                continue;
            }
            if (button.getMessage().getString().equals(OPTIONS_BUTTON_TEXT.getString())) {
                buttonX = button.getX() - LOCK_BUTTON_SIZE - 4;
                buttonY = button.getY();
                break;
            }
        }

        lockoutSettingsButton = Button.builder(
            Component.empty(),
            button -> {
                client.gui.setScreen(new LockoutSettingsScreen(this));
            }
        )
        .pos(buttonX, buttonY).width( LOCK_BUTTON_SIZE)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Lockout Settings")))
        .build();

        this.addRenderableWidget(lockoutSettingsButton);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lockout$renderLockIcon(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (lockoutSettingsButton == null || !lockoutSettingsButton.visible) {
            return;
        }

        int iconSize = 18;
        int iconX = lockoutSettingsButton.getX() + (LOCK_BUTTON_SIZE - iconSize) / 2;
        int iconY = lockoutSettingsButton.getY() + (LOCK_BUTTON_SIZE - iconSize) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, LOCK_ICON_TEXTURE, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
    }
}
