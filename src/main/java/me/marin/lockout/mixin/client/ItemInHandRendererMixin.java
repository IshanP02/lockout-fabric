package me.marin.lockout.mixin.client;

import me.marin.lockout.CompassItemHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack matrices, HumanoidArm arm, float equipProgress, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = arm == player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
        if (!CompassItemHandler.isCompass(stack)) return;

        ci.cancel();
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float)i * 0.56F, -0.52F + 0 * -0.6F, -0.72F);
    }

}
