package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class Sprint1KmGoal extends Goal implements CustomTextureRenderer {

    private static final ItemStack ITEM_STACK = Items.SUGAR.getDefaultStack();
    public Sprint1KmGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Sprint 1km";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/sprint_1km.png");
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "1km");
        return true;
    }
}