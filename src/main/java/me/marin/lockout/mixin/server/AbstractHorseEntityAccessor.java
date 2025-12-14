package me.marin.lockout.mixin.server; // Adjust to your mixin package path

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorseEntity.class)
public interface AbstractHorseEntityAccessor {

    /**
     * Access the private 'items' inventory field via a generated public method.
     */
    @Accessor("items")
    SimpleInventory getItems();
}