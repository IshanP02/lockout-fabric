package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class Obtain3UniqueHorseArmorGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.IRON_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR);
    private static final ItemStack ITEM_STACK = Items.IRON_HORSE_ARMOR.getDefaultInstance();

    public Obtain3UniqueHorseArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public int getAmount() {
        return 3;
    }

    @Override
    public String getGoalName() {
        return "Obtain 3 Unique Horse Armor";
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font,  ITEM_STACK, x, y, "3");
        return true;
    }

}
