package me.marin.lockout.mixin.server;

import me.marin.lockout.CompassItemHandler;
import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    public void onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player)) return;

        if (CompassItemHandler.isCompass(stack)) {
            cir.setReturnValue(null);
            player.getInventory().add(stack);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;
        
        // Track death time for respawn grace period
        if (lockout.isLockoutPlayer(player.getUUID())) {
            LockoutServer.playerDeathTimes.put(player.getUUID(), lockout.getTicks());
        }

        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (CompassItemHandler.isCompass(item)) {
                LockoutServer.compassHandler.compassSlots.put(player.getUUID(), i);
                return;
            }
        }
        if (CompassItemHandler.isCompass(((PlayerInventoryAccessor)player.getInventory()).getEquipment().get(EquipmentSlot.OFFHAND))) {
            LockoutServer.compassHandler.compassSlots.put(player.getUUID(), 40);
        }
    }
}
