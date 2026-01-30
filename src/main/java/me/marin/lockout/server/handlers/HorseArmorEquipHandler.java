package me.marin.lockout.server.handlers;

import me.marin.lockout.Lockout;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.wear_armor.EquipHorseWithUniqueColoredLeatherArmorGoal;
import me.marin.lockout.server.LockoutServer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import me.marin.lockout.mixin.server.AbstractHorseEntityAccessor;

public class HorseArmorEquipHandler implements UseEntityCallback {

    @Override 
    public ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }

        if (entity instanceof HorseEntity) {
            HorseEntity horse = (HorseEntity) entity;
            ItemStack heldStack = player.getStackInHand(hand);

            if(horse.isTame()) {
                if (isSpecificHorseArmor(heldStack)) {
                    // Use getBodyArmor() to check if horse already has armor
                    ItemStack currentArmor = horse.getBodyArmor();
                    if (currentArmor.isEmpty()) { 
                        // Call the logic method within this same class
                        if (player instanceof ServerPlayerEntity) {
                            checkAndCompleteHorseArmorGoal((ServerPlayerEntity) player, heldStack);
                        }
                    }
                }
            }
        }
        return ActionResult.PASS;
    }
    
    private boolean isSpecificHorseArmor(ItemStack stack) {
        return stack.getItem() == Items.LEATHER_HORSE_ARMOR ||
               stack.getItem() == Items.IRON_HORSE_ARMOR ||
               stack.getItem() == Items.GOLDEN_HORSE_ARMOR ||
               stack.getItem() == Items.DIAMOND_HORSE_ARMOR;
    }

    public static void checkAndCompleteHorseArmorGoal(ServerPlayerEntity player, ItemStack stack) {
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        DyedColorComponent dyedColor = stack.get(DataComponentTypes.DYED_COLOR);
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
