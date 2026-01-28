package me.marin.lockout.mixin.server;

import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HorseEntity.class)
public abstract class HorseEntityMixin {

    private boolean isHorseArmor(ItemStack stack) {
        return stack.getItem() == Items.LEATHER_HORSE_ARMOR ||
               stack.getItem() == Items.IRON_HORSE_ARMOR ||
               stack.getItem() == Items.GOLDEN_HORSE_ARMOR ||
               stack.getItem() == Items.DIAMOND_HORSE_ARMOR;
    }

}
