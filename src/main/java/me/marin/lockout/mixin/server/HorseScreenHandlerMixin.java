package me.marin.lockout.mixin.server;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.marin.lockout.server.handlers.HorseArmorEquipHandler;

@Mixin(HorseScreenHandler.class)
public abstract class HorseScreenHandlerMixin extends ScreenHandler {

    @Shadow private final Inventory inventory;
    @Shadow private final net.minecraft.entity.passive.AbstractHorseEntity entity;

    // Constructor MUST match the required parameters, removed the playerInventory param usage
    protected HorseScreenHandlerMixin(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, Inventory inventory, net.minecraft.entity.passive.AbstractHorseEntity entity) {
        super(null, syncId); 
        this.inventory = inventory;
        this.entity = entity;
    }

    // ... (rest of the quickMove method using getSlot(slotIndex) ) ...
    @Inject(method = "quickMove", at = @At("HEAD"))
    public void onQuickMoveHead(PlayerEntity player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            
            ItemStack stackClicked = this.getSlot(slotIndex).getStack();

            if (slotIndex >= 2 && !stackClicked.isEmpty() && isHorseArmor(stackClicked)) {
                
                // Use getBodyArmor() to check if horse already has armor
                ItemStack currentArmor = ItemStack.EMPTY;
                if (this.entity instanceof HorseEntity horse) {
                    currentArmor = horse.getBodyArmor();
                }
                
                if (currentArmor.isEmpty() || isCompatible(stackClicked, currentArmor)) {
                    System.out.println("HorseScreenHandlerMixin (HEAD): Detected valid horse armor being moved: " + stackClicked.getItem());
                    HorseArmorEquipHandler.checkAndCompleteHorseArmorGoal(serverPlayer, stackClicked);
                }
            }
        }
    }
    
    private boolean isHorseArmor(ItemStack stack) { return true; }
    private boolean isCompatible(ItemStack stack1, ItemStack stack2) { return true; }
}