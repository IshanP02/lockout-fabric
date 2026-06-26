package me.marin.lockout.mixin.server;

import me.marin.lockout.server.handlers.CopperGolemConstructionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerWorldMixin {

    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    public void onEntitySpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return; // Entity didn't spawn successfully
        
        ServerLevel world = (ServerLevel) (Object) this;
        
        // Check if it's a copper golem - adjust this condition based on your mod
        if (entity.getType() == EntityTypes.COPPER_GOLEM) {
            // Find who placed the pumpkin
            ServerPlayer constructor = CopperGolemConstructionHandler.findConstructor(
                entity.blockPosition(), 
                world
            );
            
            if (constructor != null) {
                CopperGolemConstructionHandler.onCopperGolemSpawn(constructor);
            }
        }
    }
}
