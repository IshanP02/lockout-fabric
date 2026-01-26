package me.marin.lockout.lockout.goals.misc;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;
import me.marin.lockout.lockout.texture.TextureProvider;

public class RightClickBannerWithMapGoal extends IncrementItemStatGoal implements TextureProvider{

    private static final ItemStack ITEM_STACK = Items.MAP.getDefaultStack();

    public RightClickBannerWithMapGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Stat<Item>> STATS = List.of(Stats.USED.getOrCreateStat(Items.FILLED_MAP));
    @Override
    public List<Stat<Item>> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Right Click a Banner with a Map";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/banner_map.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}