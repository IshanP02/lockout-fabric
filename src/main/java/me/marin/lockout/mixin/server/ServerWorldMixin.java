package me.marin.lockout.mixin.server;

import me.marin.lockout.server.handlers.CopperGolemConstructionHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "spawnEntity", at = @At("RETURN"))
    public void onEntitySpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return; // Entity didn't spawn successfully
        
        ServerWorld world = (ServerWorld) (Object) this;
        
        // Check if it's a copper golem - adjust this condition based on your mod
        if (entity.getType() == EntityType.COPPER_GOLEM) {
            // Find who placed the pumpkin
            ServerPlayerEntity constructor = CopperGolemConstructionHandler.findConstructor(
                entity.getBlockPos(), 
                world
            );
            
            if (constructor != null) {
                CopperGolemConstructionHandler.onCopperGolemSpawn(constructor);
            }
        }
    }
}
