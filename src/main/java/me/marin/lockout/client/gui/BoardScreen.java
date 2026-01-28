package me.marin.lockout.client.gui;

import me.marin.lockout.Lockout;
import me.marin.lockout.Utility;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.network.AnnounceGoalFocusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class BoardScreen extends HandledScreen<BoardScreenHandler> {

    private DrawContext lastDrawContext;

    public BoardScreen(BoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!Lockout.exists(LockoutClient.lockout)) {
            this.close();
            return;
        }
        this.lastDrawContext = context;
        this.renderBackground(context, mouseX, mouseY, delta);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        Utility.drawCenterBingoBoard(context, textRenderer, mouseX, mouseY);
        Goal hoveredGoal = Utility.getBoardHoveredGoal(context, mouseX, mouseY);
        if (hoveredGoal != null) {
            Utility.drawGoalInformation(context, textRenderer, hoveredGoal, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        int button = click.button();
        double mouseX = click.x();
        double mouseY = click.y();
        if (lastDrawContext == null) {
            return super.mouseClicked(click, doubleClick);
        }
        
        Goal clickedGoal = Utility.getBoardHoveredGoal(lastDrawContext, (int) mouseX, (int) mouseY);
        
        if (clickedGoal != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            
            // Check if player is on a team
            if (client.player != null && client.player.getScoreboardTeam() != null) {
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {

    }

}
