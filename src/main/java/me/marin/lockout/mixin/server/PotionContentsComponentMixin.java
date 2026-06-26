package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.DrinkPotionGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionContents.class)
public class PotionContentsComponentMixin {

    @Inject(method = "onConsume", at = @At("HEAD"))
    public void onConsume(Level world, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (!(user instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        PotionContents potionContents = (PotionContents) (Object) this;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof DrinkPotionGoal drinkPotionGoal && potionContents.potion().isPresent() && drinkPotionGoal.getPotion().equals(potionContents.potion().get())) {
                lockout.completeGoal(goal, player);
            }
        }
    }

}
