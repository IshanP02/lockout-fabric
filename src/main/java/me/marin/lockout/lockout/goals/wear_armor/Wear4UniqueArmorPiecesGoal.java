package me.marin.lockout.lockout.goals.wear_armor;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.interfaces.WearArmorGoal;
import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Wear4UniqueArmorPiecesGoal extends WearArmorGoal {

    private static final List<Item> HELMETS = List.of(Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, Items.TURTLE_HELMET);
    private static final List<Item> CHESTPLATES = List.of(Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
    private static final List<Item> LEGGINGS = List.of(Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
    private static final List<Item> BOOTS = List.of(Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
    private static final List<List<Item>> ITEMS = List.of(HELMETS, CHESTPLATES, LEGGINGS, BOOTS);

    public Wear4UniqueArmorPiecesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Wear 4 Unique Armor Pieces";
    }

    @Override
    public List<Item> getItems() {
        return null;
    }

    @Override
    public boolean satisfiedBy(PlayerInventory playerInventory) {

        var armor = new ArrayList<ItemStack>();
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.HEAD));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.CHEST));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.LEGS));
        armor.add(((PlayerInventoryAccessor)playerInventory).getEquipment().get(EquipmentSlot.FEET));

        // Check that we have 4 armor pieces
        for (ItemStack itemStack : armor) {
            if (itemStack.isEmpty()) {
                return false;
            }
        }

        // Check that all armor pieces are different materials
        var uniqueMaterials = new HashSet<String>();
        for (ItemStack itemStack : armor) {
            String material = getArmorMaterial(itemStack.getItem());
            if (material == null || !uniqueMaterials.add(material)) {
                return false;
            }
        }

        return true;
    }

    private String getArmorMaterial(Item item) {
        if (HELMETS.contains(item) || CHESTPLATES.contains(item) || LEGGINGS.contains(item) || BOOTS.contains(item)) {
            String itemName = item.toString();
            // Extract material from item name (e.g., "leather_helmet" -> "leather")
            if (itemName.contains("leather")) return "leather";
            if (itemName.contains("golden")) return "golden";
            if (itemName.contains("chainmail")) return "chainmail";
            if (itemName.contains("iron")) return "iron";
            if (itemName.contains("diamond")) return "diamond";
            if (itemName.contains("netherite")) return "netherite";
            if (itemName.contains("turtle")) return "turtle";
        }
        return null;
    }

    private int lastTickArmorChanged = -1;
    private Item armorPiece;
    @Override
    public boolean renderTexture(DrawContext context, int x, int y, int tick) {
        List<Item> itemType = ITEMS.get(tick % 240 / 60);

        int armorChange = tick / 60;
        if (lastTickArmorChanged != armorChange) {
            lastTickArmorChanged = armorChange;
            armorPiece = itemType.get(Lockout.random.nextInt(itemType.size()));
        }

        ItemStack stack = armorPiece.getDefaultStack();
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        context.drawItem(stack, x, y);
        return true;
    }

}