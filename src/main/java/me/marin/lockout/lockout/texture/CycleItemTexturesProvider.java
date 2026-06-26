package me.marin.lockout.lockout.texture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;

import java.util.List;

public interface CycleItemTexturesProvider extends CustomTextureRenderer {

    List<Item> getItemsToDisplay();

    @Override
    default boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        int mod = tick % (60 * getItemsToDisplay().size());
        context.item(getItemsToDisplay().get(mod / 60).getDefaultInstance(), x, y);
        return true;
    }

}
