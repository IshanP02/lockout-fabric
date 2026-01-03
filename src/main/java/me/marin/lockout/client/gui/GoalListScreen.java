package me.marin.lockout.client.gui;

import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.network.BroadcastPickBanPayload;
import me.marin.lockout.network.LockPickBanSelectionsPayload;
import me.marin.lockout.network.UpdatePickBanSessionPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import me.marin.lockout.client.ClientLocateUtil;
import me.marin.lockout.client.ClientPickBanSessionHolder;
import net.minecraft.sound.SoundEvents;
import me.marin.lockout.LocateData;
import me.marin.lockout.generator.GoalGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import me.marin.lockout.generator.GoalRequirements;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalListScreen extends Screen {

    private ButtonWidget closeButton;
    private ButtonWidget lockSelectionsButton;
    private BoardBuilderSearchWidget searchWidget;
    private TextFieldWidget searchTextField;
    private List<String> displayedGoals;
    private double scrollY = 0;
    private static final int ITEM_HEIGHT = 18;
    private static final int ITEM_WIDTH = 240;
    private static final int SEARCH_HEIGHT = 20;
    private DropdownGoalsPanel bansPanel;
    private DropdownGoalsPanel picksPanel;
    
    // Persist search text across GUI reopens
    private static String persistedSearchText = "";
    
    // Pick/ban session state (client-side cache)
    private UpdatePickBanSessionPayload activeSessionState = null;

    public GoalListScreen() {
        super(Text.literal("Goal List"));
        this.displayedGoals = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        
        // Load active session state from holder if it exists
        if (activeSessionState == null) {
            activeSessionState = ClientPickBanSessionHolder.getActiveSession();
        }

        int centerX = width / 2;

        // Close button at bottom - position depends on if session is active
        if (activeSessionState != null) {
            // When session is active, position buttons equidistant from center
            // Lock Selections button: 120 pixels wide, Close button: 60 pixels wide
            // Gap between them: 20 pixels
            // Total width: 60 + 20 + 120 = 200
            // Each button should be 100 pixels from center
            int closeButtonX = centerX - 100;
            closeButton = ButtonWidget.builder(Text.literal("Close"), (b) -> {
                this.close();
            }).width(60).position(closeButtonX, height - 30).build();
        } else {
            // When no session, center the close button
            closeButton = ButtonWidget.builder(Text.literal("Close"), (b) -> {
                this.close();
            }).width(60).position(centerX - 30, height - 30).build();
        }
        this.addDrawableChild(closeButton);

        // Create search/list widget similar to BoardBuilder
        int widgetX = centerX - (ITEM_WIDTH / 2);
        int widgetY = 40 + SEARCH_HEIGHT + 5;
        int widgetWidth = ITEM_WIDTH;
        int widgetHeight = height - 80 - SEARCH_HEIGHT - 5;

        searchWidget = new BoardBuilderSearchWidget(widgetX, widgetY, widgetWidth, widgetHeight, Text.empty(), true, true, persistedSearchText);
        this.addDrawableChild(searchWidget);

        // Create search bar text field
        searchTextField = new TextFieldWidget(textRenderer, widgetX, 40, widgetWidth, 18, Text.empty());
        searchTextField.setChangedListener(s -> {
            persistedSearchText = s;
            searchWidget.searchUpdated(s);
            if (s == null || s.isEmpty()) {
                searchTextField.setSuggestion("Search goals...");
            } else {
                searchTextField.setSuggestion("");
            }
        });
        searchTextField.setText(persistedSearchText);
        if (persistedSearchText.isEmpty()) {
            searchTextField.setSuggestion("Search goals...");
        }
        this.addDrawableChild(searchTextField);

        // Calculate available space for side panels
        int leftSpace = widgetX - 10; // space on left side with padding
        int rightSpace = width - (widgetX + widgetWidth) - 10; // space on right side with padding
        int panelWidth = Math.min(220, Math.max(leftSpace, rightSpace)); // max 220, but fit available space
        
        // Only show panels if there's enough space (at least 150 pixels)
        if (leftSpace >= 150) {
            // Add dropdown for banned goals to the left of the search bar
            // Combine both BANS and PENDING_BANS
            List<String> allBans = new ArrayList<>();
            allBans.addAll(GoalGroup.BANS.getGoals());
            allBans.addAll(GoalGroup.PENDING_BANS.getGoals());
            
            bansPanel = new DropdownGoalsPanel(
                widgetX - panelWidth - 10, // left of search bar with padding
                40,
                panelWidth,
                allBans,
                "Banned Goals"
            );
            this.addDrawableChild(bansPanel);
        }
        
        if (rightSpace >= 150) {
            // Add dropdown for picked goals to the right of the search bar
            // Combine both PICKS and PENDING_PICKS
            List<String> allPicks = new ArrayList<>();
            allPicks.addAll(GoalGroup.PICKS.getGoals());
            allPicks.addAll(GoalGroup.PENDING_PICKS.getGoals());
            
            picksPanel = new DropdownGoalsPanel(
                widgetX + widgetWidth + 10, // right of search bar
                40,
                panelWidth,
                allPicks,
                "Picked Goals"
            );
            this.addDrawableChild(picksPanel);
        }

        // Once the world is available, filter out goals that fail biome/structure requirements
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            // Collect all required biomes/structures from registered goals
            java.util.Set<RegistryKey<Biome>> biomeKeys = new java.util.HashSet<>();
            java.util.Set<RegistryKey<Structure>> structureKeys = new java.util.HashSet<>();
            for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
                GoalRequirements req = GoalRegistry.INSTANCE.getGoalGenerator(id);
                if (req == null) continue;
                if (req.getRequiredBiomes() != null) biomeKeys.addAll(req.getRequiredBiomes());
                if (req.getRequiredStructures() != null) structureKeys.addAll(req.getRequiredStructures());
            }

            java.util.Map<RegistryKey<Biome>, LocateData> biomes = ClientLocateUtil.locateBiomes(client, biomeKeys);
            java.util.Map<RegistryKey<Structure>, LocateData> structures = ClientLocateUtil.locateStructures(client, structureKeys);

            searchWidget.filterByRequirements(biomes, structures);
        }
        
        // Add Lock Selections button if there's an active pick/ban session
        if (activeSessionState != null) {
            int buttonWidth = 120;
            int buttonHeight = 20;
            // Position 100 pixels from center (right side), matching the Close button distance
            int buttonX = centerX + 100 - buttonWidth;
            int buttonY = height - 30; // Same height as Close button
            
            lockSelectionsButton = ButtonWidget.builder(
                Text.literal("Lock Selections"),
                button -> {
                    // Validate picks and bans before sending
                    if (client.player == null) return;
                    
                    // Use PENDING_PICKS and PENDING_BANS sizes
                    int pendingPickCount = GoalGroup.PENDING_PICKS.getGoals().size();
                    int pendingBanCount = GoalGroup.PENDING_BANS.getGoals().size();
                    int limit = activeSessionState.selectionLimit();
                    
                    // Check if counts match the limit
                    if (pendingPickCount != limit || pendingBanCount != limit) {
                        String errorMsg = "You must select exactly " + limit + " pick(s) and " + limit + " ban(s). ";
                        errorMsg += "Current: " + pendingPickCount + "/" + limit + " picks, " + pendingBanCount + "/" + limit + " bans.";
                        client.player.sendMessage(Text.literal(errorMsg).withColor(0xFF5555), false);
                        return;
                    }
                    
                    // Build goal-to-player map for pending picks and bans
                    Map<String, String> goalToPlayerMap = new HashMap<>();
                    for (String goalId : GoalGroup.PENDING_PICKS.getGoals()) {
                        String playerName = GoalGroup.getGoalPlayer(goalId);
                        if (playerName != null) {
                            goalToPlayerMap.put(goalId, playerName);
                        }
                    }
                    for (String goalId : GoalGroup.PENDING_BANS.getGoals()) {
                        String playerName = GoalGroup.getGoalPlayer(goalId);
                        if (playerName != null) {
                            goalToPlayerMap.put(goalId, playerName);
                        }
                    }
                    
                    // Send packet to server with the pending picks, bans, and player mapping
                    ClientPlayNetworking.send(new LockPickBanSelectionsPayload(
                        new ArrayList<>(GoalGroup.PENDING_PICKS.getGoals()),
                        new ArrayList<>(GoalGroup.PENDING_BANS.getGoals()),
                        goalToPlayerMap
                    ));
                }
            ).dimensions(buttonX, buttonY, buttonWidth, buttonHeight).build();
            
            this.addDrawableChild(lockSelectionsButton);
        }
    }

    private void clearSearch() {
        searchTextField.setText("");
        searchWidget.searchUpdated("");
    }

    private void toggleGoalInPicks(String goalId) {
        // Check if goal is locked in PICKS or BANS
        List<String> lockedPicks = GoalGroup.PICKS.getGoals();
        List<String> lockedBans = GoalGroup.BANS.getGoals();
        
        if (lockedPicks.contains(goalId) || lockedBans.contains(goalId)) {
            String message = activeSessionState != null 
                ? "This goal has already been locked by a team!"
                : "This goal is locked. Use /RemovePicks or /RemoveBans to unlock it.";
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal(message).withColor(0xFF5555),
                false
            );
            return;
        }
        
        // Always use PENDING groups for user selections (they only lock during sessions)
        List<String> picks = GoalGroup.PENDING_PICKS.getGoals();
        List<String> bans = GoalGroup.PENDING_BANS.getGoals();
        
        if (picks.contains(goalId)) {
            picks.remove(goalId);
            // Broadcast unpick action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "unpick"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } else {
            // Check limit during session
            if (activeSessionState != null) {
                int limit = activeSessionState.selectionLimit();
                if (picks.size() >= limit) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("You've reached the maximum number of picks (" + limit + ")");
                    MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
            } else {
                // Normal pick/ban limit when no session
                try {
                    if (picks.size() >= GoalGroup.getCustomLimit()) {
                        String goalName = formatGoalName(goalId);
                        Text message = Text.literal("Unable to add " + goalName + " to Picks as the limit has been reached.");
                        MinecraftClient.getInstance().player.sendMessage(message, false);
                        return;
                    }
                } catch (Exception e) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("Unable to add " + goalName + " to Picks as the limit has been reached.");
                    if (MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
            }
            
            picks.add(goalId);
            bans.remove(goalId);
            // Store who picked this goal
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                GoalGroup.setGoalPlayer(goalId, playerName);
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "pick"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
            }
        }
        sendPicksBansUpdate();
        refreshPanels();
    }

    private void toggleGoalInBans(String goalId) {
        // Check if goal is locked in PICKS or BANS
        List<String> lockedPicks = GoalGroup.PICKS.getGoals();
        List<String> lockedBans = GoalGroup.BANS.getGoals();
        
        if (lockedPicks.contains(goalId) || lockedBans.contains(goalId)) {
            String message = activeSessionState != null 
                ? "This goal has already been locked by a team!"
                : "This goal is locked. Use /RemovePicks or /RemoveBans to unlock it.";
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal(message).withColor(0xFF5555),
                false
            );
            return;
        }
        
        // Always use PENDING groups for user selections (they only lock during sessions)
        List<String> picks = GoalGroup.PENDING_PICKS.getGoals();
        List<String> bans = GoalGroup.PENDING_BANS.getGoals();
        
        if (bans.contains(goalId)) {
            bans.remove(goalId);
            // Broadcast unban action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "unban"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } else {
            // Check limit during session
            if (activeSessionState != null) {
                int limit = activeSessionState.selectionLimit();
                if (bans.size() >= limit) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("You've reached the maximum number of bans (" + limit + ")");
                    MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
            } else {
                // Normal pick/ban limit when no session
                try {
                    if (bans.size() >= GoalGroup.getCustomLimit()) {
                        String goalName = formatGoalName(goalId);
                        Text message = Text.literal("Unable to add " + goalName + " to Bans as the limit has been reached.");
                        MinecraftClient.getInstance().player.sendMessage(message, false);
                        return;
                    }
                } catch (Exception e) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("Unable to add " + goalName + " to Bans as the limit has been reached.");
                    if (MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
            }
            
            bans.add(goalId);
            picks.remove(goalId);
            // Store who banned this goal
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                GoalGroup.setGoalPlayer(goalId, playerName);
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "ban"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.0f));
            }
        }
        sendPicksBansUpdate();
        refreshPanels();
    }

    private void sendPicksBansUpdate() {
        // Send picks/bans update to server to broadcast to other players
        // Combine PICKS + PENDING_PICKS and BANS + PENDING_BANS for complete state
        List<String> allPicks = new ArrayList<>();
        allPicks.addAll(GoalGroup.PICKS.getGoals());
        allPicks.addAll(GoalGroup.PENDING_PICKS.getGoals());
        
        List<String> allBans = new ArrayList<>();
        allBans.addAll(GoalGroup.BANS.getGoals());
        allBans.addAll(GoalGroup.PENDING_BANS.getGoals());
        
        // Build goal-to-player map for all picks and bans
        Map<String, String> goalToPlayerMap = new HashMap<>();
        for (String goalId : allPicks) {
            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                goalToPlayerMap.put(goalId, playerName);
            }
        }
        for (String goalId : allBans) {
            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                goalToPlayerMap.put(goalId, playerName);
            }
        }
        
        ClientPlayNetworking.send(new UpdatePicksBansPayload(
            allPicks,
            allBans,
            goalToPlayerMap
        ));
    }

    public void refreshPanels() {
        // Remove old panels
        if (bansPanel != null) {
            this.remove(bansPanel);
        }
        if (picksPanel != null) {
            this.remove(picksPanel);
        }

        // Re-create panels with updated lists
        int centerX = width / 2;
        int widgetX = centerX - (ITEM_WIDTH / 2);
        int widgetWidth = ITEM_WIDTH;
        
        // Calculate available space for side panels
        int leftSpace = widgetX - 10;
        int rightSpace = width - (widgetX + widgetWidth) - 10;
        int panelWidth = Math.min(220, Math.max(leftSpace, rightSpace));
        
        // Only show panels if there's enough space
        if (leftSpace >= 150) {
            // Combine both BANS and PENDING_BANS
            List<String> allBans = new ArrayList<>();
            allBans.addAll(GoalGroup.BANS.getGoals());
            allBans.addAll(GoalGroup.PENDING_BANS.getGoals());
            
            bansPanel = new DropdownGoalsPanel(
                widgetX - panelWidth - 10,
                40,
                panelWidth,
                allBans,
                "Banned Goals"
            );
            this.addDrawableChild(bansPanel);
        }

        if (rightSpace >= 150) {
            // Combine both PICKS and PENDING_PICKS
            List<String> allPicks = new ArrayList<>();
            allPicks.addAll(GoalGroup.PICKS.getGoals());
            allPicks.addAll(GoalGroup.PENDING_PICKS.getGoals());
            
            picksPanel = new DropdownGoalsPanel(
                widgetX + widgetWidth + 10,
                40,
                panelWidth,
                allPicks,
                "Picked Goals"
            );
            this.addDrawableChild(picksPanel);
        }
    }

    private String formatGoalName(String goalId) {
        // Convert GOAL_ID to "Goal Id" format using the same capitalization as GUI
        return WordUtils.capitalize(goalId.replace("_", " ").toLowerCase(), ' ');
    }

    private void loadGoals() {
        displayedGoals.clear();
        displayedGoals.addAll(GoalRegistry.INSTANCE.getRegisteredGoals());
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Let the searchWidget render entries (it handles scissoring and icons)
        if (searchWidget != null) {
            searchWidget.render(context, mouseX, mouseY, delta);
        }

        // Render search text field
        if (searchTextField != null) {
            searchTextField.render(context, mouseX, mouseY, delta);
        }
        
        // Draw ALL overlays and text AFTER widgets to ensure they're on top
        
        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw team selection banner if session is active
        if (activeSessionState != null) {
            String activeTeamName = activeSessionState.isTeam1Turn() ? 
                activeSessionState.team1Name() : activeSessionState.team2Name();
            String bannerText = activeTeamName + " is Now Selecting";
            
            // Get team color from scoreboard
            MinecraftClient client = MinecraftClient.getInstance();
            int teamColor = 0xFFFFFFFF; // default white with full alpha
            if (client.world != null && client.world.getScoreboard() != null) {
                net.minecraft.scoreboard.Team team = client.world.getScoreboard().getTeam(activeTeamName);
                if (team != null && team.getColor() != null && team.getColor().getColorValue() != null) {
                    int colorValue = team.getColor().getColorValue();
                    // Force ARGB - getColorValue() returns RGB only
                    teamColor = 0xFF000000 | colorValue;
                    // Ensure color is not black (0x000000)
                    if ((teamColor & 0x00FFFFFF) == 0) {
                        teamColor = 0xFFFFFFFF;
                    }
                }
            }
            
            // Draw background box for the banner
            int textWidth = this.textRenderer.getWidth(bannerText);
            int textHeight = this.textRenderer.fontHeight;
            int bannerX = (width - textWidth) / 2;
            int bannerY = 24;
            int padding = 4;
            
            // Semi-transparent dark background
            context.fill(bannerX - padding, bannerY - padding, 
                        bannerX + textWidth + padding, bannerY + textHeight + padding, 
                        0xAA000000);
            
            // Draw the text using the same method as the working round text
            context.drawText(this.textRenderer, bannerText, bannerX, bannerY, teamColor, true);
        }
        
        // Render pick/ban session info if active
        if (activeSessionState != null) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            
            // Display round info at the top
            String roundText = "Round " + activeSessionState.currentRound() + "/" + activeSessionState.maxRounds();
            int roundX = (width - textRenderer.getWidth(roundText)) / 2;
            context.drawText(textRenderer, roundText, roundX, 10, 0xFFFFFF, true);
            
            // Display active team
            String activeTeamName = activeSessionState.isTeam1Turn() ? 
                activeSessionState.team1Name() : activeSessionState.team2Name();
            String turnText = activeTeamName + "'s Turn";
            int turnX = (width - textRenderer.getWidth(turnText)) / 2;
            context.drawText(textRenderer, turnText, turnX, 22, 0xFFAA00, true);
            
            // Get pending counts from PENDING_PICKS and PENDING_BANS
            int pendingPicks = GoalGroup.PENDING_PICKS.getGoals().size();
            int pendingBans = GoalGroup.PENDING_BANS.getGoals().size();
            int limit = activeSessionState.selectionLimit();
            
            int pickColor = (pendingPicks == limit) ? 0x55FF55 : 0xFFFFFF;
            int banColor = (pendingBans == limit) ? 0x55FF55 : 0xFFFFFF;
            
            // Draw picks part
            String picksText = "Picks: " + pendingPicks + "/" + limit;
            int progressX = (width - textRenderer.getWidth(picksText + " | Bans: " + pendingBans + "/" + limit)) / 2;
            context.drawText(textRenderer, picksText, progressX, height - 75, pickColor, true);
            
            // Draw bans part
            String bansText = " | Bans: " + pendingBans + "/" + limit;
            int bansX = progressX + textRenderer.getWidth(picksText);
            context.drawText(textRenderer, bansText, bansX, height - 75, banColor, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check team validation if there's an active session
        if (activeSessionState != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                String activeTeamName = activeSessionState.isTeam1Turn() ? 
                    activeSessionState.team1Name() : activeSessionState.team2Name();
                
                net.minecraft.scoreboard.Team playerTeam = mc.player.getScoreboardTeam();
                if (playerTeam == null) {
                    mc.player.sendMessage(
                        Text.literal("You are not on a team!").withColor(0xFF5555),
                        false
                    );
                    return super.mouseClicked(mouseX, mouseY, button); // Only allow button clicks
                }
                
                if (!playerTeam.getName().equals(activeTeamName)) {
                    return super.mouseClicked(mouseX, mouseY, button); // Only allow button clicks
                }
            }
        }
        
        // Intercept clicks on the search widget to handle PICKS/BANS
        int widgetX = (width - ITEM_WIDTH) / 2;
        int widgetY = 40 + SEARCH_HEIGHT + 5;
        int widgetWidth = ITEM_WIDTH;

        // Check if click is within the search widget area
        if (mouseX >= widgetX && mouseX <= widgetX + widgetWidth &&
            mouseY >= widgetY && mouseY <= widgetY + (height - 80 - SEARCH_HEIGHT - 5)) {

            // If the click is on the scrollbar region, let the widget handle it instead
            int widgetHeight = (height - 80 - SEARCH_HEIGHT - 5);
            int scrollbarStartX = widgetX + widgetWidth - 6; // scrollbar sits on the right edge
            if (mouseX >= scrollbarStartX && mouseX <= widgetX + widgetWidth && mouseY >= widgetY && mouseY <= widgetY + widgetHeight) {
                if (searchWidget != null) {
                    if (searchWidget.mouseClicked(mouseX, mouseY, button)) return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            // Ask the widget which goal (if any) is under the mouse
            if (searchWidget != null) {
                String goalId = ((BoardBuilderSearchWidget) searchWidget).getGoalIdAtPosition(mouseX, mouseY);
                if (goalId != null) {
                    if (button == 0) { // Left click
                        toggleGoalInPicks(goalId);
                        return true;
                    } else if (button == 1) { // Right click
                        toggleGoalInBans(goalId);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private java.util.List<String> getVisibleGoals() {
        // Return filtered goals from the search widget
        java.util.List<String> visible = new java.util.ArrayList<>();
        String searchText = searchTextField != null ? searchTextField.getText() : "";
        for (String goalId : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            if (goalId.toLowerCase().contains(searchText.toLowerCase())) {
                visible.add(goalId);
            }
        }
        return visible;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        // If the mouse is over the search widget, delegate to it so mouse wheel works correctly
        int widgetX = (width - ITEM_WIDTH) / 2;
        int widgetY = 40 + SEARCH_HEIGHT + 5;
        int widgetWidth = ITEM_WIDTH;
        int widgetHeight = height - 80 - SEARCH_HEIGHT - 5;

        if (mouseX >= widgetX && mouseX <= widgetX + widgetWidth && mouseY >= widgetY && mouseY <= widgetY + widgetHeight) {
            if (searchWidget != null) {
                if (searchWidget.mouseScrolled(mouseX, mouseY, horizontal, vertical)) return true;
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (searchWidget != null) {
            if (searchWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (searchWidget != null) {
            if (searchWidget.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchTextField != null && searchTextField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchTextField != null && searchTextField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public void refreshForPickBanSession(UpdatePickBanSessionPayload sessionState) {
        this.activeSessionState = sessionState;
        
        // Refresh the GUI
        this.clearAndInit();
    }

}
