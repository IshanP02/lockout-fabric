package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.opponent.OpponentHitByEggGoal;
import me.marin.lockout.lockout.goals.opponent.OpponentHitBySnowballGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin({Snowball.class, ThrownEgg.class})
public class ThrowableProjectileMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    protected void onProjectileHitEntity(EntityHitResult result, CallbackInfo ci) {
        if (!(result.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ThrowableItemProjectile projectile = (ThrowableItemProjectile) (Object) this;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof OpponentHitBySnowballGoal) {
                if (projectile instanceof Snowball && projectile.getOwner() instanceof Player shooter && !Objects.equals(player, shooter)) {
                    lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with a Snowball.");
                }
            }
            if (goal instanceof OpponentHitByEggGoal) {
                if (projectile instanceof ThrownEgg && projectile.getOwner() instanceof Player shooter && !Objects.equals(player, shooter)) {
                    lockout.complete1v1Goal(goal, shooter, true, shooter.getName().getString() + " hit " + player.getName().getString() + " with an Egg.");
                }
            }
        }
    }
}
