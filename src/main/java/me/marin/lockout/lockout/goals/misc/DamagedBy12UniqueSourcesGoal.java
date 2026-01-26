package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class DamagedBy12UniqueSourcesGoal extends DamagedByUniqueSourcesGoal {

    private static final ItemStack ITEM_STACK = Items.TERRACOTTA.getDefaultStack();

    public DamagedBy12UniqueSourcesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Take Damage from 12 Unique Sources";
    }

    @Override
    public int getAmount() {
        return 12;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, ITEM_STACK, x, y, "12");
        return true;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/take_unique_damage.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}