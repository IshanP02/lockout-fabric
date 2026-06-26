package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.TootGoatHornGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InstrumentItem.class)
public class GoatHornItemMixin {

    @Inject(method = "use", at = @At(value = "INVOKE", target="Lnet/minecraft/world/item/InstrumentItem;play(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/Instrument;)V", shift = At.Shift.AFTER))
    public void onUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (user.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof TootGoatHornGoal) {
                lockout.completeGoal(goal, user);
            }
        }
    }

}
