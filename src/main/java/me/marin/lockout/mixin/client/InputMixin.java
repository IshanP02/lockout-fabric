package me.marin.lockout.mixin.client;

import me.marin.lockout.Lockout;
import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class InputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (!Lockout.isLockoutRunning(LockoutClient.lockout)) return;
        if (!LockoutClient.amIPlayingLockout) return;

        if (!LockoutClient.lockout.hasStarted()) {
            keyPresses = new Input(false, false, false, false, false, keyPresses.shift(), false);
        }
    }

}