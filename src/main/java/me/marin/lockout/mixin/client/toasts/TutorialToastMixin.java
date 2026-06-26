package me.marin.lockout.mixin.client.toasts;

import me.marin.lockout.Lockout;
import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TutorialToast.class)
public class TutorialToastMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void onDraw(GuiGraphicsExtractor context, Font font, long startTime, CallbackInfo ci) {
        if (Lockout.exists(LockoutClient.lockout)) {
            ci.cancel();
        }
    }

}
