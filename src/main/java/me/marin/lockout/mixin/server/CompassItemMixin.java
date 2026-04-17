package me.marin.lockout.mixin.server;

import me.marin.lockout.CompassItemHandler;
import me.marin.lockout.Lockout;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
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
    public void onInventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot, CallbackInfo ci) {
        if (world.isClient()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!(entity instanceof PlayerEntity player)) return;
        if (!lockout.isLockoutPlayer(player)) return;

        if (!CompassItemHandler.isCompass(stack)) return;
        CompassItemHandler handler = LockoutServer.getOrInitCompassHandler();
        if (handler == null) return;

        int selectionNum = handler.currentSelection.getOrDefault(player.getUuid(), -1);
        if (selectionNum < 0) return;
        if (selectionNum >= handler.players.size()) return;
        UUID selectedId = handler.players.get(selectionNum);
        PlayerEntity selectedPlayer = world.getServer().getPlayerManager().getPlayer(selectedId);
        if (selectedPlayer != null) {
            if (selectedPlayer.getEntityWorld().equals(player.getEntityWorld())) {
                stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(GlobalPos.create(world.getRegistryKey(), selectedPlayer.getBlockPos())), true));
                ci.cancel();
            }
        } else {
            stack.remove(DataComponentTypes.LODESTONE_TRACKER);
        }

        Integer trackingIndex = handler.currentSelection.get(player.getUuid());
        if (trackingIndex == null || trackingIndex < 0 || trackingIndex >= handler.players.size()) return;
        UUID trackingUuid = handler.players.get(trackingIndex);
        String trackingPlayerName = handler.playerNames.getOrDefault(trackingUuid, "Unknown");

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(Formatting.RESET + "Tracking: " +  trackingPlayerName));
    }

}
