package me.marin.lockout.lockout.goals.status_effect.applied_for_x_minutes;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.RequiresAmount;
import me.marin.lockout.lockout.interfaces.HaveEffectsAppliedForXMinutesGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

public class HaveEffectsAppliedFor5MinutesGoal extends HaveEffectsAppliedForXMinutesGoal implements RequiresAmount, TextureProvider, CustomTextureRenderer {

    private final static ItemStack DISPLAY_ITEM_STACK = Items.DYE.brown().getDefaultInstance();
    static {
        DISPLAY_ITEM_STACK.setCount(5);
    }

    public HaveEffectsAppliedFor5MinutesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Have Effects Applied for 5 Minutes";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public int getAmount() {
        return 5;
    }

    @Override
    public int getMinutes() {
        return 5;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/status_effect/applied_effects.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.itemDecorations(Minecraft.getInstance().font, DISPLAY_ITEM_STACK, x, y, "5");
        return true;
    }

}
