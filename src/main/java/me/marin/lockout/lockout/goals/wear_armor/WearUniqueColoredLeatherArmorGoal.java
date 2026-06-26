package me.marin.lockout.lockout.goals.wear_armor;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.interfaces.WearArmorGoal;
import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WearUniqueColoredLeatherArmorGoal extends WearArmorGoal {

    private static final List<Item> ITEMS = List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);

    public WearUniqueColoredLeatherArmorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Wear Full Leather Armor, in different colors";
    }

    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    @Override
    public boolean satisfiedBy(Inventory playerInventory) {
        if (!super.satisfiedBy(playerInventory)) return false;

        var armor = new ArrayList<ItemStack>();
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.HEAD));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.CHEST));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.LEGS));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.FEET));

        Set<Integer> colors = new HashSet<>();
        for (ItemStack itemStack : armor) {
            DyedItemColor dyedColor = itemStack.get(DataComponents.DYED_COLOR);
            if (dyedColor == null) continue;
            colors.add(dyedColor.rgb());
        }

        return colors.size() == 4;
    }

    private int lastTickColorChanged = -1;
    private int color = 0;
    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        int mod = tick % (60 * getItemsToDisplay().size());
        ItemStack itemStack = getItemsToDisplay().get(mod / 60).getDefaultInstance();

        int colorChange = tick / 60;
        if (lastTickColorChanged != colorChange) {
            lastTickColorChanged = colorChange;
            color = (Lockout.random.nextInt(0, 256) << 16) | (Lockout.random.nextInt(0, 256) << 8) | (Lockout.random.nextInt(0, 256));
        }

        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        context.item(itemStack, x, y);
        return true;
    }

}
