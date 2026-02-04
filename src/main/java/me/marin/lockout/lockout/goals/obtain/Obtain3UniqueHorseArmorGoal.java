package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain3UniqueHorseArmorGoal extends ObtainSomeOfTheItemsGoal {

    private static final List<Item> ITEMS = List.of(Items.IRON_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR);
    private static final ItemStack ITEM_STACK = Items.IRON_HORSE_ARMOR.getDefaultStack();

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
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "3");
        return true;
    }

}
