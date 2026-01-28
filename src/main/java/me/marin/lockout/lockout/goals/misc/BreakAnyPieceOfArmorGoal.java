package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.List;

public class BreakAnyPieceOfArmorGoal extends IncrementItemStatGoal implements CycleItemTexturesProvider {

    private static final Item ITEM = Items.TURTLE_HELMET;
    private static final List<Item> ITEMS = List.of(
        
        Items.LEATHER_BOOTS,
        Items.LEATHER_LEGGINGS,
        Items.LEATHER_CHESTPLATE,
        Items.LEATHER_HELMET,
        Items.COPPER_BOOTS,
        Items.COPPER_LEGGINGS,
        Items.COPPER_CHESTPLATE,
        Items.COPPER_HELMET,
        Items.CHAINMAIL_BOOTS,
        Items.CHAINMAIL_LEGGINGS,
        Items.CHAINMAIL_CHESTPLATE,
        Items.CHAINMAIL_HELMET,
        Items.IRON_BOOTS,
        Items.IRON_LEGGINGS,
        Items.IRON_CHESTPLATE,
        Items.IRON_HELMET,
        Items.GOLDEN_BOOTS,
        Items.GOLDEN_LEGGINGS,
        Items.GOLDEN_CHESTPLATE,
        Items.GOLDEN_HELMET,
        Items.DIAMOND_BOOTS,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_HELMET,
        Items.NETHERITE_BOOTS,
        Items.NETHERITE_LEGGINGS,
        Items.NETHERITE_CHESTPLATE,
        Items.NETHERITE_HELMET,
        Items.TURTLE_HELMET

    );

    private static final List<Stat<Item>> STATS = List.of(
        Stats.BROKEN.getOrCreateStat(Items.LEATHER_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.LEATHER_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.LEATHER_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.LEATHER_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.COPPER_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.COPPER_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.COPPER_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.COPPER_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.CHAINMAIL_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.CHAINMAIL_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.CHAINMAIL_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.CHAINMAIL_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.IRON_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.IRON_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.IRON_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.IRON_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.GOLDEN_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.GOLDEN_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.GOLDEN_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.GOLDEN_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.DIAMOND_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.DIAMOND_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.DIAMOND_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.DIAMOND_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.NETHERITE_BOOTS),
        Stats.BROKEN.getOrCreateStat(Items.NETHERITE_LEGGINGS),
        Stats.BROKEN.getOrCreateStat(Items.NETHERITE_CHESTPLATE),
        Stats.BROKEN.getOrCreateStat(Items.NETHERITE_HELMET),
        Stats.BROKEN.getOrCreateStat(Items.TURTLE_HELMET)
        
    );

    public BreakAnyPieceOfArmorGoal(String id, String data) {
        super(id, data);
    }

    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultStack();
    }

     @Override
    public List<Stat<Item>> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Break Any Piece of Armor";
    }

    @Override
    public List<Item> getItemsToDisplay() {
        return ITEMS;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/durability_bar.png");
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        int mod = tick % (60 * getItemsToDisplay().size());
        context.drawItem(ITEMS.get(mod / 60).getDefaultStack(), x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }

}
