package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.Utility;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.network.AnnounceGoalFocusPayload;
import me.marin.lockout.network.RequestGoalDetailsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

public class BoardScreen extends AbstractContainerScreen<BoardScreenHandler> {

    private GuiGraphicsExtractor lastDrawContext;

    public BoardScreen(BoardScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!Lockout.exists(LockoutClient.lockout)) {
            this.onClose();
            return;
        }
        this.lastDrawContext = context;
        this.extractBackground(context, mouseX, mouseY, delta);
        Font textRenderer = Minecraft.getInstance().font;

        Utility.drawCenterBingoBoard(context, font, mouseX, mouseY, false);
        Goal hoveredGoal = Utility.getBoardHoveredGoal(context, mouseX, mouseY, false);
        if (hoveredGoal != null) {
            Utility.drawGoalInformation(context, font, hoveredGoal, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        int button = click.button();
        double mouseX = click.x();
        double mouseY = click.y();
        if (lastDrawContext == null) {
            return super.mouseClicked(click, doubleClick);
        }
        
        Goal clickedGoal = Utility.getBoardHoveredGoal(lastDrawContext, (int) mouseX, (int) mouseY, false);
        
        if (clickedGoal != null) {
            Minecraft client = Minecraft.getInstance();
            
            // Check if player is a spectator (not on any team)
            if (client.player != null && client.player.getTeam() == null) {
                // Spectator: request detailed goal info
                ClientPlayNetworking.send(new RequestGoalDetailsPayload(clickedGoal.getId()));
                return true;
            }
            
            // Team player: announce working on goal
            if (client.player != null && client.player.getTeam() != null) {
                // Left-click (button 0) = working on, Right-click (button 1) = reminder
                boolean isReminder = (button == 1);
                
                // Send packet to server
                ClientPlayNetworking.send(new AnnounceGoalFocusPayload(clickedGoal.getId(), isReminder));
                
                return true;
            }
        }
        
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {

    }

}
