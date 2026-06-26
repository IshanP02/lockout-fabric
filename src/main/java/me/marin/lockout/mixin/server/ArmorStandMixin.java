package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.misc.FillArmorStandGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(ArmorStand.class)
public class ArmorStandMixin {

    @Inject(method = "interact", at = @At("RETURN"))
    public void onInteractAt(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ArmorStand armorStand = (ArmorStand) (Object) this;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (!(goal instanceof FillArmorStandGoal fillArmorStandGoal)) continue;
            if (goal.isCompleted()) continue;

            // TODO: Do better
            var armor = new ArrayList<ItemStack>();
            armor.add(armorStand.getItemBySlot(EquipmentSlot.HEAD));
            armor.add(armorStand.getItemBySlot(EquipmentSlot.CHEST));
            armor.add(armorStand.getItemBySlot(EquipmentSlot.LEGS));
            armor.add(armorStand.getItemBySlot(EquipmentSlot.FEET));

            if (!serverPlayer.isSpectator() && cir.getReturnValue() == InteractionResult.SUCCESS_SERVER) {
                for (ItemStack armorItem : armor) {
                    if (armorItem == null || armorItem.isEmpty()) return;
                }
                // Armor stand is now full
                lockout.completeGoal(fillArmorStandGoal, player);
                return;
            }
        }


    }

}
