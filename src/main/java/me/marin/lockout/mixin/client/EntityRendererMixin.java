package me.marin.lockout.mixin.client;

import me.marin.lockout.client.RenderedEntityCounter;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks the number of entities actually rendered each frame.
 * Fixes the broken E: counter in 1.21.11 debug HUD.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void lockout$countRenderedEntity(
            EntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue commandQueue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        RenderedEntityCounter.increment();
    }
}
