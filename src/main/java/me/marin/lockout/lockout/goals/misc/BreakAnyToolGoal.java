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

public class BreakAnyToolGoal extends IncrementItemStatGoal implements CycleItemTexturesProvider {

    private static final Item ITEM = Items.IRON_PICKAXE;
    private static final List<Item> ITEMS = List.of(
        
        Items.WOODEN_AXE,
        Items.WOODEN_HOE,
        Items.WOODEN_PICKAXE,
        Items.WOODEN_SHOVEL,
        Items.WOODEN_SWORD,
        Items.WOODEN_SPEAR,
        Items.STONE_AXE,
        Items.STONE_HOE,
        Items.STONE_PICKAXE,
        Items.STONE_SHOVEL,
        Items.STONE_SWORD,
        Items.STONE_SPEAR,
        Items.COPPER_AXE,
        Items.COPPER_HOE,
        Items.COPPER_PICKAXE,
        Items.COPPER_SHOVEL,
        Items.COPPER_SWORD,
        Items.COPPER_SPEAR,
        Items.GOLDEN_AXE,
        Items.GOLDEN_HOE,
        Items.GOLDEN_PICKAXE,
        Items.GOLDEN_SHOVEL,
        Items.GOLDEN_SWORD,
        Items.GOLDEN_SPEAR,
        Items.IRON_AXE,
        Items.IRON_HOE,
        Items.IRON_PICKAXE,
        Items.IRON_SHOVEL,
        Items.IRON_SWORD,
        Items.IRON_SPEAR,
        Items.DIAMOND_AXE,
        Items.DIAMOND_HOE,
        Items.DIAMOND_PICKAXE,
        Items.DIAMOND_SHOVEL,
        Items.DIAMOND_SWORD,
        Items.DIAMOND_SPEAR,
        Items.NETHERITE_AXE,
        Items.NETHERITE_HOE,
        Items.NETHERITE_PICKAXE,
        Items.NETHERITE_SHOVEL,
        Items.NETHERITE_SWORD,
        Items.NETHERITE_SPEAR,
        Items.SHEARS,
        Items.FISHING_ROD,
        Items.CARROT_ON_A_STICK,
        Items.WARPED_FUNGUS_ON_A_STICK,
        Items.FLINT_AND_STEEL,
        Items.BOW,
        Items.CROSSBOW,
        Items.TRIDENT,
        Items.SHIELD,
        Items.BRUSH

    );

    private static final List<Stat<Item>> STATS = List.of(
        Stats.ITEM_USED.get(Items.WOODEN_AXE),
        Stats.ITEM_USED.get(Items.WOODEN_HOE),
        Stats.ITEM_USED.get(Items.WOODEN_PICKAXE),
        Stats.ITEM_USED.get(Items.WOODEN_SHOVEL),
        Stats.ITEM_USED.get(Items.WOODEN_SWORD),
        Stats.ITEM_USED.get(Items.WOODEN_SPEAR),
        Stats.ITEM_USED.get(Items.STONE_AXE),
        Stats.ITEM_USED.get(Items.STONE_HOE),
        Stats.ITEM_USED.get(Items.STONE_PICKAXE),
        Stats.ITEM_USED.get(Items.STONE_SHOVEL),
        Stats.ITEM_USED.get(Items.STONE_SWORD),
        Stats.ITEM_USED.get(Items.STONE_SPEAR),
        Stats.ITEM_USED.get(Items.COPPER_AXE),
        Stats.ITEM_USED.get(Items.COPPER_HOE),
        Stats.ITEM_USED.get(Items.COPPER_PICKAXE),
        Stats.ITEM_USED.get(Items.COPPER_SHOVEL),
        Stats.ITEM_USED.get(Items.COPPER_SWORD),
        Stats.ITEM_USED.get(Items.COPPER_SPEAR),
        Stats.ITEM_USED.get(Items.GOLDEN_AXE),
        Stats.ITEM_USED.get(Items.GOLDEN_HOE),
        Stats.ITEM_USED.get(Items.GOLDEN_PICKAXE),
        Stats.ITEM_USED.get(Items.GOLDEN_SHOVEL),
        Stats.ITEM_USED.get(Items.GOLDEN_SWORD),
        Stats.ITEM_USED.get(Items.GOLDEN_SPEAR),
        Stats.ITEM_USED.get(Items.IRON_AXE),
        Stats.ITEM_USED.get(Items.IRON_HOE),
        Stats.ITEM_USED.get(Items.IRON_PICKAXE),
        Stats.ITEM_USED.get(Items.IRON_SHOVEL),
        Stats.ITEM_USED.get(Items.IRON_SWORD),
        Stats.ITEM_USED.get(Items.IRON_SPEAR),
        Stats.ITEM_USED.get(Items.DIAMOND_AXE),
        Stats.ITEM_USED.get(Items.DIAMOND_HOE),
        Stats.ITEM_USED.get(Items.DIAMOND_PICKAXE),
        Stats.ITEM_USED.get(Items.DIAMOND_SHOVEL),
        Stats.ITEM_USED.get(Items.DIAMOND_SWORD),
        Stats.ITEM_USED.get(Items.DIAMOND_SPEAR),
        Stats.ITEM_USED.get(Items.NETHERITE_AXE),
        Stats.ITEM_USED.get(Items.NETHERITE_HOE),
        Stats.ITEM_USED.get(Items.NETHERITE_PICKAXE),
        Stats.ITEM_USED.get(Items.NETHERITE_SHOVEL),
        Stats.ITEM_USED.get(Items.NETHERITE_SWORD),
        Stats.ITEM_USED.get(Items.NETHERITE_SPEAR),
        Stats.ITEM_USED.get(Items.SHEARS),
        Stats.ITEM_USED.get(Items.FISHING_ROD),
        Stats.ITEM_USED.get(Items.CARROT_ON_A_STICK),
        Stats.ITEM_USED.get(Items.WARPED_FUNGUS_ON_A_STICK),
        Stats.ITEM_USED.get(Items.FLINT_AND_STEEL),
        Stats.ITEM_USED.get(Items.BOW),
        Stats.ITEM_USED.get(Items.CROSSBOW),
        Stats.ITEM_USED.get(Items.TRIDENT),
        Stats.ITEM_USED.get(Items.SHIELD),
        Stats.ITEM_USED.get(Items.BRUSH),
        Stats.ITEM_USED.get(Items.MACE)
    );

    public BreakAnyToolGoal(String id, String data) {
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
        return "Break Any Tool";
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
