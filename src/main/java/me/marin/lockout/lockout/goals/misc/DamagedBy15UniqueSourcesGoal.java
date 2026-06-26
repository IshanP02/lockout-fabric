package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

public class DamagedBy15UniqueSourcesGoal extends DamagedByUniqueSourcesGoal {

    private static final ItemStack ITEM_STACK = Items.TERRACOTTA.getDefaultInstance();

    public DamagedBy15UniqueSourcesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Take Damage from 15 Unique Sources";
    }

    @Override
    public int getAmount() {
        return 15;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y, "15");
        return true;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/take_unique_damage.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}