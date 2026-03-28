package me.marin.lockout.mixin.client;

import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Unique
    private boolean lockout$f3Held = false;
    @Unique
    private boolean lockout$f3ComboUsed = false;

    /**
     * On pure F3 release (no combo key pressed while F3 was held), toggle our own
     * lockoutDebugHudOpen flag. This mirrors what Minecraft does internally with
     * switchF3State, without reading shouldShowDebugHud() which can be unreliable
     * when keys are pressed in rapid succession.
     */
    @Inject(method = "onKey", at = @At("HEAD"))
    private void lockout$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (input.key() == GLFW.GLFW_KEY_F3) {
            if (action == GLFW.GLFW_PRESS) {
                lockout$f3Held = true;
                lockout$f3ComboUsed = false;
            } else if (action == GLFW.GLFW_RELEASE) {
                if (!lockout$f3ComboUsed) {
                    LockoutClient.lockoutDebugHudOpen = !LockoutClient.lockoutDebugHudOpen;
                }
                lockout$f3Held = false;
                lockout$f3ComboUsed = false;
            }
        } else if (action == GLFW.GLFW_PRESS && lockout$f3Held) {
            lockout$f3ComboUsed = true;
        }
    }
}
