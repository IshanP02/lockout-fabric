package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.NameMobGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public class NameTagItemMixin {

    @Inject(method = "interactLivingEntity", at = @At("RETURN"))
    public void onNameTagUse(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (playerIn.level().isClientSide()) return;
        if (!(playerIn instanceof ServerPlayer player)) return;
        if (entity instanceof Player) return;

        // Naming only happens when the name tag carries a custom name
        Component tagName = stack.get(DataComponents.CUSTOM_NAME);
        if (tagName == null) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        String nameString = tagName.getString();

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof NameMobGoal nameMobGoal) {
                if (nameMobGoal.matchesEntity(entity) && nameString.equals(nameMobGoal.getRequiredName())) {
                    lockout.completeGoal(nameMobGoal, player);
                }
            }
        }
    }
}
