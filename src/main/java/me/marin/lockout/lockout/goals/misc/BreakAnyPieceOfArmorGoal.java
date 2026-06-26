package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.IncrementItemStatGoal;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

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
        Stats.ITEM_BROKEN.get(Items.LEATHER_BOOTS),
        Stats.ITEM_BROKEN.get(Items.LEATHER_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.LEATHER_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.LEATHER_HELMET),
        Stats.ITEM_BROKEN.get(Items.COPPER_BOOTS),
        Stats.ITEM_BROKEN.get(Items.COPPER_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.COPPER_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.COPPER_HELMET),
        Stats.ITEM_BROKEN.get(Items.CHAINMAIL_BOOTS),
        Stats.ITEM_BROKEN.get(Items.CHAINMAIL_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.CHAINMAIL_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.CHAINMAIL_HELMET),
        Stats.ITEM_BROKEN.get(Items.IRON_BOOTS),
        Stats.ITEM_BROKEN.get(Items.IRON_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.IRON_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.IRON_HELMET),
        Stats.ITEM_BROKEN.get(Items.GOLDEN_BOOTS),
        Stats.ITEM_BROKEN.get(Items.GOLDEN_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.GOLDEN_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.GOLDEN_HELMET),
        Stats.ITEM_BROKEN.get(Items.DIAMOND_BOOTS),
        Stats.ITEM_BROKEN.get(Items.DIAMOND_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.DIAMOND_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.DIAMOND_HELMET),
        Stats.ITEM_BROKEN.get(Items.NETHERITE_BOOTS),
        Stats.ITEM_BROKEN.get(Items.NETHERITE_LEGGINGS),
        Stats.ITEM_BROKEN.get(Items.NETHERITE_CHESTPLATE),
        Stats.ITEM_BROKEN.get(Items.NETHERITE_HELMET),
        Stats.ITEM_BROKEN.get(Items.TURTLE_HELMET)
        
    );

    public BreakAnyPieceOfArmorGoal(String id, String data) {
        super(id, data);
    }

    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
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

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/durability_bar.png");
    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        int mod = tick % (60 * getItemsToDisplay().size());
        context.item(ITEMS.get(mod / 60).getDefaultInstance(), x, y);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0,0, 16, 16, 16, 16);
        return true;
    }

}
