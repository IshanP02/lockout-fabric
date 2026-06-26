package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.wear_armor.EquipHorseWithUniqueColoredLeatherArmorGoal;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import me.marin.lockout.mixin.server.AbstractHorseEntityAccessor;

public class HorseArmorEquipHandler implements UseEntityCallback {

    @Override 
    public InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            ItemStack heldStack = player.getItemInHand(hand);

            if(horse.isTamed()) {
                if (isSpecificHorseArmor(heldStack)) {
                    // Use getItemBySlot(BODY) to check if horse already has armor
                    ItemStack currentArmor = horse.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY);
                    if (currentArmor.isEmpty()) { 
                        // Call the logic method within this same class
                        if (player instanceof ServerPlayer) {
                            checkAndCompleteHorseArmorGoal((ServerPlayer) player, heldStack);
                        }
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }
    
    private boolean isSpecificHorseArmor(ItemStack stack) {
        return stack.getItem() == Items.LEATHER_HORSE_ARMOR ||
               stack.getItem() == Items.IRON_HORSE_ARMOR ||
               stack.getItem() == Items.GOLDEN_HORSE_ARMOR ||
               stack.getItem() == Items.DIAMOND_HORSE_ARMOR;
    }

    public static void checkAndCompleteHorseArmorGoal(ServerPlayer player, ItemStack stack) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor == null) return;

        int color = dyedColor.rgb();

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null || goal.isCompleted()) continue;

            if (goal instanceof EquipHorseWithUniqueColoredLeatherArmorGoal) {
                EquipHorseWithUniqueColoredLeatherArmorGoal armorGoal = (EquipHorseWithUniqueColoredLeatherArmorGoal) goal;

                if (armorGoal.getColorValue() == color && armorGoal.getItemType() == stack.getItem()) {
                    lockout.completeGoal(goal, player);
                    break;
                }
            }
        }
    }
}
