package me.marin.lockout.lockout.goals.obtain;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.ObtainItemsGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.mixin.server.PlayerInventoryAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ObtainShieldWithBannerGoal extends ObtainItemsGoal implements TextureProvider {

    public ObtainShieldWithBannerGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Decorate Shield with Banner";
    }

    @Override
    public List<Item> getItems() {
        return null;
    }

    @Override
    public boolean satisfiedBy(Inventory playerInventory) {
        for (ItemStack item : ((PlayerInventoryAccessor) playerInventory).getPlayerInventory()) {
            if (item == null) continue;
            if (item.isEmpty()) continue;
            if (!item.getItem().equals(Items.SHIELD)) continue;

            if (item.get(DataComponents.BASE_COLOR) != null) {
                return true;
            }
        }

        var offHandItem = ((PlayerInventoryAccessor) playerInventory).getEquipment().get(EquipmentSlot.OFFHAND);
        return offHandItem != null && !offHandItem.isEmpty() && offHandItem.getItem().equals(Items.SHIELD)
                && offHandItem.get(DataComponents.BASE_COLOR) != null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/apply_banner_shield.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        return TextureProvider.super.renderTexture(context, x, y, tick);
    }
}
