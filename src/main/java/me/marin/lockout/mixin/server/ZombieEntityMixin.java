package me.marin.lockout.mixin.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public class ZombieEntityMixin {

    @Unique
    private Difficulty before;

    @Inject(method = "killedEntity", at = @At("HEAD"))
    public void setDifficulty(ServerLevel world, LivingEntity other, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        before = world.getDifficulty();
        world.getServer().setDifficulty(Difficulty.HARD, true);
    }

    @Inject(method = "killedEntity", at = @At("RETURN"))
    public void restoreDifficulty(ServerLevel world, LivingEntity other, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        world.getServer().setDifficulty(before, true);
    }

}
