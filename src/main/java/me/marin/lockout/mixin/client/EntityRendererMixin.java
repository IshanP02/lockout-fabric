package me.marin.lockout.mixin.client;

import me.marin.lockout.client.RenderedEntityCounter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks the number of entities actually rendered each frame.
 * Fixes the broken E: counter in debug HUD.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
        method = "extractRenderState",
        at = @At("HEAD")
    )
    private void lockout$countRenderedEntity(
            Entity entity,
            EntityRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        RenderedEntityCounter.increment();
    }
}
