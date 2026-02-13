package me.marin.lockout.mixin.server;

import me.marin.lockout.server.handlers.CopperGolemConstructionHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin {

    @Inject(method = "onUseWithItem", at = @At("HEAD"))
    private void onPumpkinSheared(
        ItemStack stack,
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        Hand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        
        if (stack.isOf(Items.SHEARS)) {
            CopperGolemConstructionHandler.recordPumpkinAction(pos, serverPlayer, world);
        }
    }
}
