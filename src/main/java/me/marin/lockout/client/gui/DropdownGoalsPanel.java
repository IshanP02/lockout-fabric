package me.marin.lockout.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.opengl.GlStateManager;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.Constants;
import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.generator.GoalGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import me.marin.lockout.client.LockoutClient;
// skin rendering uses the client's AbstractClientPlayer#getSkinTexture()
import net.minecraft.world.scores.Team;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.entity.player.PlayerSkin;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DropdownGoalsPanel extends AbstractWidget {
    private final List<String> goals;
    private final String label;
    private final Map<String, CachedGoalEntry> cachedGoals = new HashMap<>();
    private final Map<String, Identifier> flagCache = new HashMap<>();
    private static final int LABEL_HEIGHT = 22;
    private static final int ITEM_HEIGHT = 18;
    private static final Identifier CHECKMARK_TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/gui/sprites/checkmark.png");
    private static final Identifier BARRIER_TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/gui/sprites/barrier.png");

    public DropdownGoalsPanel(int x, int y, int width, List<String> goals, String label) {
        super(x, y, width, LABEL_HEIGHT + goals.size() * ITEM_HEIGHT, Component.literal(label));
        this.goals = goals;
        this.label = label;
    }

    /**
     * Notify the panel that a goal was assigned - no longer caches, kept for compatibility.
     */
    public void onGoalAssigned(String goalId, String playerName) {
        // Caching removed - we render directly from the player entity
        if (playerName == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        // Cache team flag if available
        var maybePlayer = client.level.players().stream()
                .filter(p -> p.getName().getString().equals(playerName))
                .findFirst();
        
        if (maybePlayer.isPresent()) {
            Team team = maybePlayer.get().getTeam();
            if (team != null && team.getColor().isPresent()) {
                Identifier flag = getFlagTexture(team.getColor().get());
                if (flag != null) flagCache.put(playerName, flag);
            }
        }
    }

    @Override
    public int getHeight() {
        return LABEL_HEIGHT + goals.size() * ITEM_HEIGHT + 1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;

        // Colors
        int backgroundColor = label.contains("Picked") ? 0xFF1a4d1a : 0xFF4d1a1a;
        int borderColor = label.contains("Picked") ? 0xFF55FF55 : 0xFFFF5555;

        // Background + border
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), backgroundColor);
        context.fill(getX(), getY(), getX() + getWidth(), getY() + 1, borderColor);
        context.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), borderColor);
        context.fill(getX(), getY() + 1, getX() + 1, getY() + getHeight() - 1, borderColor);
        context.fill(getX() + getWidth() - 1, getY() + 1, getX() + getWidth(), getY() + getHeight() - 1, borderColor);

        // Center label + icon
        int textWidth = font.width(label);
        int iconWidth = 16;
        int spacing = 4;
        int totalWidth = iconWidth + spacing + textWidth;
        int startX = getX() + (getWidth() - totalWidth) / 2;

        Identifier iconTexture = label.contains("Picked") ? CHECKMARK_TEXTURE : BARRIER_TEXTURE;
        context.blit(RenderPipelines.GUI_TEXTURED, iconTexture, startX, getY() + 3, 0, 0, 16, 16, 16, 16);
        context.text(font, label, startX + iconWidth + spacing, getY() + 6, Color.WHITE.getRGB(), true);

        int yOffset = getY() + LABEL_HEIGHT;

        for (String goalId : goals) {
            CachedGoalEntry cachedEntry = cachedGoals.computeIfAbsent(goalId, CachedGoalEntry::new);
            cachedEntry.goal.render(context, font, getX() + 6, yOffset + 1);
            context.text(font, cachedEntry.displayName, getX() + 24, yOffset + 6, Color.WHITE.getRGB(), true);

            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                renderPlayerHead(context, playerName, getX() + getWidth() - 18, yOffset + 1);
            }

            yOffset += ITEM_HEIGHT;
        }
    }


    private void renderPlayerHead(GuiGraphicsExtractor context, String playerName, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        // First try to find the player in the world (for players nearby)
        var player = client.level.players().stream()
                .filter(p -> p.getName().getString().equals(playerName))
                .findFirst()
                .orElse(null);

        PlayerSkin PlayerSkin = null;
        net.minecraft.client.multiplayer.PlayerInfo playerListEntry = null;
        
        if (player != null && player instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
            // Player is in world, use their skin directly
            PlayerSkin = clientPlayer.getSkin();
        }

        if (PlayerSkin == null) return;

        int size = 16;
        
        // Use Minecraft's PlayerSkinDrawer to render the face and hat overlay
        PlayerFaceExtractor.extractRenderState(context, PlayerSkin, x, y, size);

        // Team flag (from local cache if present)
        Identifier flagTexture = flagCache.get(playerName);
        if (flagTexture == null) {
            Team team = null;
            if (player != null) {
                team = player.getTeam();
            } else if (playerListEntry != null && client.level != null) {
                // Get team from scoreboard using player name
                var scoreboard = client.level.getScoreboard();
                team = scoreboard.getPlayerTeam(playerName);
            }
            
            if (team != null && team.getColor().isPresent()) {
                flagTexture = getFlagTexture(team.getColor().get());
            }
        }
        if (flagTexture != null) {
            context.blit(RenderPipelines.GUI_TEXTURED, flagTexture, x - 6, y - 3, 0, 0, 12, 12, 12, 12);
        }
    }


        private static Identifier getFlagTexture(TeamColor teamColor) {
            String colorName = teamColor.getSerializedName();
            return Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/flags/" + colorName + "_flag.png");
        }

        public void onClick(double mouseX, double mouseY) {
            // No click behavior needed
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            // No narration needed
        }

        private static class CachedGoalEntry {
            final Goal goal;
            final String displayName;

            CachedGoalEntry(String goalId) {
                Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(goalId);
                String data = gen.map(g -> g.generateData(new ArrayList<>(GoalDataGenerator.ALL_DYES))).orElse(GoalDataConstants.DATA_NONE);
                this.goal = GoalRegistry.INSTANCE.newGoal(goalId, data);

                String goalName;
                if (gen.isEmpty()) {
                    goalName = this.goal.getGoalName();
                } else {
                    goalName = "[*] " + WordUtils.capitalize(goalId.replace("_", " ").toLowerCase(), ' ');
                }

                this.displayName = truncateGoalName(goalName);
            }
        }

        private static String truncateGoalName(String goalName) {
            final int MAX_LENGTH = 30;
            if (goalName.length() > MAX_LENGTH) {
                int endIndex = Math.min(MAX_LENGTH - 1, goalName.length());
                return goalName.substring(0, endIndex) + "…";
            }
            return goalName;
        }
    }