package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.wear_armor.PutWolfArmorOnWolfGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public class WolfEntityMixin {

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/wolf/Wolf;setItemSlotAndDropWhenKilled(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 0))
    public void onEquipArmor(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof PutWolfArmorOnWolfGoal) {
                lockout.completeGoal(goal, player);
                return;
            }
        }
    }

}
