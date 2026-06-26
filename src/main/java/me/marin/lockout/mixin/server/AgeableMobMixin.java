package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.UseGoldenDandelionOnBabyMobGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AgeableMob.class)
public class AgeableMobMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    public void onGoldenDandelionInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) return;
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        AgeableMob self = (AgeableMob)(Object) this;
        if (!self.isBaby()) return;
        // Item stack count may be 0 after consume but the item type reference is still intact
        if (player.getItemInHand(hand).getItem() != Items.GOLDEN_DANDELION) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof UseGoldenDandelionOnBabyMobGoal) {
                lockout.completeGoal(goal, serverPlayer);
            }
        }
    }
}
