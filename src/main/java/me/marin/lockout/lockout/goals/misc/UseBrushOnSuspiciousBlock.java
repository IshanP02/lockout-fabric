package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class UseBrushOnSuspiciousBlock extends Goal implements CustomTextureRenderer {

    public UseBrushOnSuspiciousBlock(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Brush on Suspicious Sand/Gravel";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return Items.BRUSH.getDefaultInstance();
    }

    private static final List<ItemStack> SUSPICIOUS_BLOCKS = List.of(Items.SUSPICIOUS_GRAVEL.getDefaultInstance(), Items.SUSPICIOUS_SAND.getDefaultInstance());
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/brush_overlay.png");
    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        int mod = tick % (60 * SUSPICIOUS_BLOCKS.size());
        context.item(SUSPICIOUS_BLOCKS.get(mod / 60), x, y);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }

}
