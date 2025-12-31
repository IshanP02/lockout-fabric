package me.marin.lockout.lockout.goals.status_effect.applied_for_x_minutes;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.RequiresAmount;
import me.marin.lockout.lockout.interfaces.HaveEffectsAppliedForXMinutesGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class HaveEffectsAppliedFor10MinutesGoal extends HaveEffectsAppliedForXMinutesGoal implements RequiresAmount, TextureProvider, CustomTextureRenderer {

    private final static ItemStack DISPLAY_ITEM_STACK = Items.BROWN_DYE.getDefaultStack();
    static {
        DISPLAY_ITEM_STACK.setCount(10);
    }

    public HaveEffectsAppliedFor10MinutesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have Effects Applied for 10 Minutes";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public int getAmount() {
        return 10;
    }

    @Override
    public int getMinutes() {
        return 10;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/status_effect/applied_effects.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, DISPLAY_ITEM_STACK, x, y, "10");
        return true;
    }

}
