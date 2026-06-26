package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.workstation.UseGrindstoneGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public class GrindstoneScreenHandlerOutputSlotMixin {

    @Inject(method="onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    public void onTakeItem(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof UseGrindstoneGoal) {
                if (player.containerMenu instanceof GrindstoneMenu grindstoneScreenHandler) {
                    if ((Object) this == grindstoneScreenHandler.slots.get(2)) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }

        }
    }

}
