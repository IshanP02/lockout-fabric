package me.marin.lockout.mixin.client;

import me.marin.lockout.client.RenderedEntityCounter;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resets the rendered entity counter at the start of each frame.
 * Uses GameRenderer instead of WorldRenderer for better stability across versions.
 */
@Mixin(GameRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void lockout$resetEntityCounter(
            RenderTickCounter tickCounter,
            boolean renderLevel,
            CallbackInfo ci
    ) {
        RenderedEntityCounter.reset();
    }
}
