package me.marin.lockout.mixin.server;

import me.marin.lockout.CompassItemHandler;
import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(CompassItem.class)
public class CompassItemMixin {

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    public void onInventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot, CallbackInfo ci) {
        if (world.isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(entity instanceof Player player)) return;
        if (!lockout.isLockoutPlayer(player)) return;

        if (!CompassItemHandler.isCompass(stack)) return;

        CompassItem item = (CompassItem) stack.getItem();
        int selectionNum = LockoutServer.compassHandler.currentSelection.getOrDefault(player.getUUID(), -1);
        if (selectionNum < 0) return;
        UUID selectedId = LockoutServer.compassHandler.players.get(selectionNum);
        Player selectedPlayer = world.getServer().getPlayerList().getPlayer(selectedId);
        if (selectedPlayer != null) {
            if (selectedPlayer.level().equals(player.level())) {
                stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(world.dimension(), selectedPlayer.blockPosition())), true));
                ci.cancel();
            }
        } else {
            stack.remove(DataComponents.LODESTONE_TRACKER);
        }

        CompassItemHandler cih = LockoutServer.compassHandler;
        String trackingPlayerName = cih.playerNames.get(cih.players.get(cih.currentSelection.get(player.getUUID())));

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(ChatFormatting.RESET + "Tracking: " +  trackingPlayerName));
    }

}
