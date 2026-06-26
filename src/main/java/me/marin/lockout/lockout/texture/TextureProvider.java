package me.marin.lockout.lockout.texture;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public interface TextureProvider extends CustomTextureRenderer {

    Identifier getTextureIdentifier();

    @Override
    default boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, getTextureIdentifier(), x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }

}
