package me.marin.lockout.mixin.server;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raid.class)
public class RaidMixin {

    @Inject(method = "getNumGroups", at = @At("HEAD"), cancellable = true)
    public void getMaxWaves(Difficulty difficulty, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(5);
    }

    @Redirect(method = "getPotentialBonusSpawns", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DifficultyInstance;getDifficulty()Lnet/minecraft/world/Difficulty;"))
    public Difficulty setDifficulty(DifficultyInstance instance) {
        return Difficulty.NORMAL;
    }

}
