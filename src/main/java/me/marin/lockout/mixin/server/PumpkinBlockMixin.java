package me.marin.lockout.mixin.server;

import me.marin.lockout.server.handlers.CopperGolemConstructionHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onPumpkinSheared(
        ItemStack stack,
        BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (world.isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        if (stack.is(Items.SHEARS)) {
            CopperGolemConstructionHandler.recordPumpkinAction(pos, serverPlayer, world);
        }
    }
}
