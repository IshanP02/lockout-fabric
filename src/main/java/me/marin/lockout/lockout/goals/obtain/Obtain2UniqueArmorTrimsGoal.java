package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.lockout.interfaces.ObtainSomeOfTheItemsGoal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class Obtain2UniqueArmorTrimsGoal extends ObtainSomeOfTheItemsGoal {

    private static final ItemStack ITEM_STACK = Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack();
    static {
        ITEM_STACK.setCount(2);
    }
    private static final List<Item> ITEMS = List.of(
            Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE
    );

    public Obtain2UniqueArmorTrimsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public int getAmount() {
        return 2;
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public String getGoalName() {
        return "Obtain 2 Unique Armor Trims";
    }

    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        super.renderTexture(context, x, y, tick);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer,  ITEM_STACK, x, y, "2");
        return true;
    }
}
