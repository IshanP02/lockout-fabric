package me.marin.lockout.mixin.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.marin.lockout.server.handlers.HorseArmorEquipHandler;

@Mixin(HorseScreenHandler.class)
public abstract class HorseScreenHandlerMixin extends ScreenHandler {

    // This is the ONLY shadow field you should have:
    @Shadow private final Inventory inventory; 

    // Constructor MUST match the required parameters, removed the playerInventory param usage
    protected HorseScreenHandlerMixin(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, Inventory inventory, Object entity) {
        super(null, syncId); 
        this.inventory = inventory;
        // NO reference to playerInventory field here either
    }

    // ... (rest of the quickMove method using getSlot(slotIndex) ) ...
    @Inject(method = "quickMove", at = @At("HEAD"))
    public void onQuickMoveHead(PlayerEntity player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        // ... (your implementation using this.getSlot(slotIndex) and this.inventory.getStack(1)) ...
        if (player instanceof ServerPlayerEntity serverPlayer) {
            
            ItemStack stackClicked = this.getSlot(slotIndex).getStack();

            if (slotIndex >= 2 && !stackClicked.isEmpty() && isHorseArmor(stackClicked)) {
                
                ItemStack horseArmorSlotStack = this.inventory.getStack(1);
                
                if (horseArmorSlotStack.isEmpty() || isCompatible(stackClicked, horseArmorSlotStack)) {
                    System.out.println("HorseScreenHandlerMixin (HEAD): Detected valid horse armor being moved: " + stackClicked.getItem());
                    HorseArmorEquipHandler.checkAndCompleteHorseArmorGoal(serverPlayer, stackClicked);
                }
            }
        }
    }
    
    private boolean isHorseArmor(ItemStack stack) { return true; }
    private boolean isCompatible(ItemStack stack1, ItemStack stack2) { return true; }
}