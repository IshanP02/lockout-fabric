package me.marin.lockout.mixin.server;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorse.class)
public interface AbstractHorseEntityAccessor {

    @Accessor("inventory")
    SimpleContainer getItems();
}
