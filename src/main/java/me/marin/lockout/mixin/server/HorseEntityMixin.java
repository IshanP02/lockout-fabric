package me.marin.lockout.mixin.server;

import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Horse.class)
public abstract class HorseEntityMixin {

    private boolean isHorseArmor(ItemStack stack) {
        return stack.getItem() == Items.LEATHER_HORSE_ARMOR ||
               stack.getItem() == Items.IRON_HORSE_ARMOR ||
               stack.getItem() == Items.GOLDEN_HORSE_ARMOR ||
               stack.getItem() == Items.DIAMOND_HORSE_ARMOR;
    }

}
