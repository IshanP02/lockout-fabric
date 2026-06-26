package me.marin.lockout.lockout.goals.misc;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;
import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;
import me.marin.lockout.lockout.texture.TextureProvider;

public class RightClickBannerWithMapGoal extends IncrementItemStatGoal implements TextureProvider{

    private static final ItemStack ITEM_STACK = Items.MAP.getDefaultInstance();

    public RightClickBannerWithMapGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Stat<Item>> STATS = List.of(Stats.ITEM_USED.get(Items.FILLED_MAP));
    @Override
    public List<Stat<Item>> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Create a Waypoint by using a Map on a Banner";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        return true;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/banner_map.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}