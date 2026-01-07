package me.marin.lockout.mixin.server;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.screen.HorseScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseScreenHandler.class)
public interface HorseScreenHandlerAccessor {

    /**
     * Access the private 'entity' field via a generated public method.
     */
    @Accessor("entity")
    AbstractHorseEntity getEntity();
}
