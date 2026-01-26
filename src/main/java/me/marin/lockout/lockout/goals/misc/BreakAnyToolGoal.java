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

public class BreakAnyToolGoal extends IncrementItemStatGoal implements CycleItemTexturesProvider {

    private static final Item ITEM = Items.IRON_PICKAXE;
    private static final List<Item> ITEMS = List.of(
        
        Items.WOODEN_AXE,
        Items.WOODEN_HOE,
        Items.WOODEN_PICKAXE,
        Items.WOODEN_SHOVEL,
        Items.WOODEN_SWORD,
        Items.STONE_AXE,
        Items.STONE_HOE,
        Items.STONE_PICKAXE,
        Items.STONE_SHOVEL,
        Items.STONE_SWORD,
        Items.GOLDEN_AXE,
        Items.GOLDEN_HOE,
        Items.GOLDEN_PICKAXE,
        Items.GOLDEN_SHOVEL,
        Items.GOLDEN_SWORD,
        Items.IRON_AXE,
        Items.IRON_HOE,
        Items.IRON_PICKAXE,
        Items.IRON_SHOVEL,
        Items.IRON_SWORD,
        Items.DIAMOND_AXE,
        Items.DIAMOND_HOE,
        Items.DIAMOND_PICKAXE,
        Items.DIAMOND_SHOVEL,
        Items.DIAMOND_SWORD,
        Items.NETHERITE_AXE,
        Items.NETHERITE_HOE,
        Items.NETHERITE_PICKAXE,
        Items.NETHERITE_SHOVEL,
        Items.NETHERITE_SWORD,
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
        Stats.USED.getOrCreateStat(Items.WOODEN_AXE),
        Stats.USED.getOrCreateStat(Items.WOODEN_HOE),
        Stats.USED.getOrCreateStat(Items.WOODEN_PICKAXE),
        Stats.USED.getOrCreateStat(Items.WOODEN_SHOVEL),
        Stats.USED.getOrCreateStat(Items.WOODEN_SWORD),
        Stats.USED.getOrCreateStat(Items.STONE_AXE),
        Stats.USED.getOrCreateStat(Items.STONE_HOE),
        Stats.USED.getOrCreateStat(Items.STONE_PICKAXE),
        Stats.USED.getOrCreateStat(Items.STONE_SHOVEL),
        Stats.USED.getOrCreateStat(Items.STONE_SWORD),
        Stats.USED.getOrCreateStat(Items.GOLDEN_AXE),
        Stats.USED.getOrCreateStat(Items.GOLDEN_HOE),
        Stats.USED.getOrCreateStat(Items.GOLDEN_PICKAXE),
        Stats.USED.getOrCreateStat(Items.GOLDEN_SHOVEL),
        Stats.USED.getOrCreateStat(Items.GOLDEN_SWORD),
        Stats.USED.getOrCreateStat(Items.IRON_AXE),
        Stats.USED.getOrCreateStat(Items.IRON_HOE),
        Stats.USED.getOrCreateStat(Items.IRON_PICKAXE),
        Stats.USED.getOrCreateStat(Items.IRON_SHOVEL),
        Stats.USED.getOrCreateStat(Items.IRON_SWORD),
        Stats.USED.getOrCreateStat(Items.DIAMOND_AXE),
        Stats.USED.getOrCreateStat(Items.DIAMOND_HOE),
        Stats.USED.getOrCreateStat(Items.DIAMOND_PICKAXE),
        Stats.USED.getOrCreateStat(Items.DIAMOND_SHOVEL),
        Stats.USED.getOrCreateStat(Items.DIAMOND_SWORD),
        Stats.USED.getOrCreateStat(Items.NETHERITE_AXE),
        Stats.USED.getOrCreateStat(Items.NETHERITE_HOE),
        Stats.USED.getOrCreateStat(Items.NETHERITE_PICKAXE),
        Stats.USED.getOrCreateStat(Items.NETHERITE_SHOVEL),
        Stats.USED.getOrCreateStat(Items.NETHERITE_SWORD),
        Stats.USED.getOrCreateStat(Items.SHEARS),
        Stats.USED.getOrCreateStat(Items.FISHING_ROD),
        Stats.USED.getOrCreateStat(Items.CARROT_ON_A_STICK),
        Stats.USED.getOrCreateStat(Items.WARPED_FUNGUS_ON_A_STICK),
        Stats.USED.getOrCreateStat(Items.FLINT_AND_STEEL),
        Stats.USED.getOrCreateStat(Items.BOW),
        Stats.USED.getOrCreateStat(Items.CROSSBOW),
        Stats.USED.getOrCreateStat(Items.TRIDENT),
        Stats.USED.getOrCreateStat(Items.SHIELD),
        Stats.USED.getOrCreateStat(Items.BRUSH),
        Stats.USED.getOrCreateStat(Items.MACE)
    );

    public BreakAnyToolGoal(String id, String data) {
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
        return "Break Any Tool";
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
