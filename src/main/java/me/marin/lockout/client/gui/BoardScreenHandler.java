package me.marin.lockout.client.gui;


import me.marin.lockout.client.LockoutClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class BoardScreenHandler extends AbstractContainerMenu {

    public BoardScreenHandler(int syncId, @SuppressWarnings("unused") Inventory playerInventory) {
        super(LockoutClient.BOARD_SCREEN_HANDLER, syncId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

}