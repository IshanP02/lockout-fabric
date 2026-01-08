package me.marin.lockout.client.gui;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.Constants;
import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.generator.GoalGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import org.apache.commons.lang3.text.WordUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DropdownGoalsPanel extends ClickableWidget {
    private final List<String> goals;
    private final String label;
    private final Map<String, CachedGoalEntry> cachedGoals = new HashMap<>();
    private static final int LABEL_HEIGHT = 22;
    private static final int ITEM_HEIGHT = 18;
    private static final Identifier CHECKMARK_TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/gui/sprites/checkmark.png");
    private static final Identifier BARRIER_TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/gui/sprites/barrier.png");

    public DropdownGoalsPanel(int x, int y, int width, List<String> goals, String label) {
        super(x, y, width, LABEL_HEIGHT + goals.size() * ITEM_HEIGHT, Text.literal(label));
        this.goals = goals;
        this.label = label;
    }

    @Override
    public int getHeight() {
        // Dynamically calculate height based on current goals list size, with 2px bottom padding
        return LABEL_HEIGHT + goals.size() * ITEM_HEIGHT + 1;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Determine background color based on label
        int backgroundColor = label.contains("Picked") ? 0xFF1a4d1a : 0xFF4d1a1a; // darker green for picks, darker red for bans
        int borderColor = label.contains("Picked") ? 0xFF55FF55 : 0xFFFF5555; // bright green for picks, bright red for bans
        
        // Draw background
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), backgroundColor);
        
        // Draw border
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), borderColor);
        
        // Calculate centered position for icon + text
        int textWidth = textRenderer.getWidth(label);
        int iconWidth = 16;
        int spacing = 4;
        int totalWidth = iconWidth + spacing + textWidth;
        int startX = getX() + (getWidth() - totalWidth) / 2;
        
        // Draw icon centered
        Identifier iconTexture = label.contains("Picked") ? CHECKMARK_TEXTURE : BARRIER_TEXTURE;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture, startX, getY() + 3, 0, 0, 16, 16, 16, 16);
        
        // Draw label text centered after icon
        context.drawTextWithShadow(textRenderer, label, startX + iconWidth + spacing, getY() + 6, Color.WHITE.getRGB());
        
        // Draw all goals
        int yOffset = getY() + LABEL_HEIGHT;
        for (String goalId : goals) {
            // Cache goal entry on-demand to handle dynamic goal additions
            CachedGoalEntry cachedEntry = cachedGoals.computeIfAbsent(goalId, CachedGoalEntry::new);
            cachedEntry.goal.render(context, textRenderer, getX() + 6, yOffset + 1);
            context.drawTextWithShadow(textRenderer, cachedEntry.displayName, getX() + 24, yOffset + 6, Color.WHITE.getRGB());
            
            // Draw player head if we know who picked/banned this goal
            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                renderPlayerHead(context, playerName, getX() + getWidth() - 18, yOffset + 1);
            }
            
            yOffset += ITEM_HEIGHT;
        }
    }
    
    private void renderPlayerHead(DrawContext context, String playerName, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        // Try to find the player in the current world
        var player = client.world.getPlayers().stream()
            .filter(p -> p.getName().getString().equals(playerName))
            .findFirst()
            .orElse(null);
        
        if (player != null) {
            // Draw the player's head (16x16 from their skin texture)
            var skinTexture = client.getSkinProvider().getSkinTextures(player.getGameProfile());
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture.texture(), x, y, 8.0F, 8.0F, 16, 16, 8, 8, 64, 64);
            // Draw the overlay (hat layer)
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skinTexture.texture(), x, y, 40.0F, 8.0F, 16, 16, 8, 8, 64, 64);
            
            // Get the player's team and draw the flag
            net.minecraft.scoreboard.Team team = player.getScoreboardTeam();
            if (team != null && team.getColor() != null) {
                Identifier flagTexture = getFlagTexture(team.getColor());
                if (flagTexture != null) {
                    // Draw flag in top left corner (8x8 pixels)
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, flagTexture, x - 6, y - 3, 0, 0, 12, 12, 12, 12);
                }
            }
        }
    }
    
    private static Identifier getFlagTexture(Formatting teamColor) {
        String colorName = teamColor.asString().toLowerCase();
        return Identifier.of(Constants.NAMESPACE, "textures/custom/flags/" + colorName + "_flag.png");
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        // No click behavior needed - always showing the list
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // No narration needed
    }
    
    private static class CachedGoalEntry {
        final Goal goal;
        final String displayName;
        
        CachedGoalEntry(String goalId) {
            Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(goalId);
            String data = gen.map(g -> g.generateData(new ArrayList<>(GoalDataGenerator.ALL_DYES))).orElse(GoalDataConstants.DATA_NONE);
            this.goal = GoalRegistry.INSTANCE.newGoal(goalId, data);
            
            // Use the same display name logic as BoardBuilderSearchWidget
            String goalName;
            if (gen.isEmpty()) {
                goalName = this.goal.getGoalName();
            } else {
                goalName = "[*] " + WordUtils.capitalize(goalId.replace("_", " ").toLowerCase(), ' ');
            }
            
            // Truncate all goal names longer than 20 characters
            this.displayName = truncateGoalName(goalName);
        }
    }
    
    private static String truncateGoalName(String goalName) {
        // Max characters that fit in the panel (accounting for texture + spacing)
        // With 200px width, texture (16px) + spacing (8px), leaves ~176px for text
        // At roughly 8-9 pixels per character, max is around 20 characters
        final int MAX_LENGTH = 30;
        if (goalName.length() > MAX_LENGTH) {
            int endIndex = Math.min(MAX_LENGTH - 1, goalName.length());
            return goalName.substring(0, endIndex) + "…";
        }
        return goalName;
    }
}
