package me.marin.lockout.client.gui;

import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.network.BroadcastPickBanPayload;
import me.marin.lockout.network.UpdatePicksBansPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import me.marin.lockout.client.ClientLocateUtil;
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
    private BoardBuilderSearchWidget searchWidget;
    private TextFieldWidget searchTextField;
    private List<String> displayedGoals;
    private double scrollY = 0;
    private static final int ITEM_HEIGHT = 18;
    private static final int ITEM_WIDTH = 240;
    private static final int SEARCH_HEIGHT = 20;
    private DropdownGoalsPanel bansPanel;
    private DropdownGoalsPanel picksPanel;

    public GoalListScreen() {
        super(Text.literal("Goal List"));
        this.displayedGoals = new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;

        // Close button at bottom
        closeButton = ButtonWidget.builder(Text.literal("Close"), (b) -> {
            this.close();
        }).width(60).position(centerX - 30, height - 30).build();
        this.addDrawableChild(closeButton);

        // Create search/list widget similar to BoardBuilder
        int widgetX = centerX - (ITEM_WIDTH / 2);
        int widgetY = 40 + SEARCH_HEIGHT + 5;
        int widgetWidth = ITEM_WIDTH;
        int widgetHeight = height - 80 - SEARCH_HEIGHT - 5;

        searchWidget = new BoardBuilderSearchWidget(widgetX, widgetY, widgetWidth, widgetHeight, Text.empty(), true, true);
        this.addDrawableChild(searchWidget);

        // Create search bar text field
        searchTextField = new TextFieldWidget(textRenderer, widgetX, 40, widgetWidth, 18, Text.empty());
        searchTextField.setChangedListener(s -> {
            searchWidget.searchUpdated(s);
            if (s == null || s.isEmpty()) {
                searchTextField.setSuggestion("Search goals...");
            } else {
                searchTextField.setSuggestion("");
            }
        });
        searchTextField.setSuggestion("Search goals...");
        this.addDrawableChild(searchTextField);

        // Calculate available space for side panels
        int leftSpace = widgetX - 10; // space on left side with padding
        int rightSpace = width - (widgetX + widgetWidth) - 10; // space on right side with padding
        int panelWidth = Math.min(220, Math.max(leftSpace, rightSpace)); // max 220, but fit available space
        
        // Only show panels if there's enough space (at least 150 pixels)
        if (leftSpace >= 150) {
            // Add dropdown for banned goals to the left of the search bar
            bansPanel = new DropdownGoalsPanel(
                widgetX - panelWidth - 10, // left of search bar with padding
                40,
                panelWidth,
                GoalGroup.BANS.getGoals(),
                "Banned Goals"
            );
            this.addDrawableChild(bansPanel);
        }
        
        if (rightSpace >= 150) {
            // Add dropdown for picked goals to the right of the search bar
            picksPanel = new DropdownGoalsPanel(
                widgetX + widgetWidth + 10, // right of search bar
                40,
                panelWidth,
                GoalGroup.PICKS.getGoals(),
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
    }

    private void clearSearch() {
        searchTextField.setText("");
        searchWidget.searchUpdated("");
    }

    private void toggleGoalInPicks(String goalId) {
        List<String> picks = GoalGroup.PICKS.getGoals();
        List<String> bans = GoalGroup.BANS.getGoals();
        if (picks.contains(goalId)) {
            picks.remove(goalId);
            // Broadcast unpick action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "unpick"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } else {
            try {
                if (picks.size() >= GoalGroup.getCustomLimit()) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("Unable to add " + goalName + " to Picks as the limit has been reached.");
                    MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
                picks.add(goalId);
                // Store who picked this goal
                if (MinecraftClient.getInstance().player != null) {
                    String playerName = MinecraftClient.getInstance().player.getName().getString();
                    GoalGroup.setGoalPlayer(goalId, playerName);
                }
            } catch (Exception e) {
                String goalName = formatGoalName(goalId);
                Text message = Text.literal("Unable to add " + goalName + " to Picks as the limit has been reached.");
                if (MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.sendMessage(message, false);
                return;
            }
            bans.remove(goalId);
            // Broadcast pick action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "pick"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
            }
        }
        sendPicksBansUpdate();
    }

    private void toggleGoalInBans(String goalId) {
        List<String> picks = GoalGroup.PICKS.getGoals();
        List<String> bans = GoalGroup.BANS.getGoals();
        if (bans.contains(goalId)) {
            bans.remove(goalId);
            // Broadcast unban action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "unban"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
        } else {
            try {
                if (bans.size() >= GoalGroup.getCustomLimit()) {
                    String goalName = formatGoalName(goalId);
                    Text message = Text.literal("Unable to add " + goalName + " to Bans as the limit has been reached.");
                    MinecraftClient.getInstance().player.sendMessage(message, false);
                    return;
                }
                bans.add(goalId);                // Store who banned this goal
                if (MinecraftClient.getInstance().player != null) {
                    String playerName = MinecraftClient.getInstance().player.getName().getString();
                    GoalGroup.setGoalPlayer(goalId, playerName);
                }            } catch (Exception e) {
                String goalName = formatGoalName(goalId);
                Text message = Text.literal("Unable to add " + goalName + " to Bans as the limit has been reached.");
                if (MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.sendMessage(message, false);
                return;
            }
            picks.remove(goalId);
            // Broadcast ban action to all players
            if (MinecraftClient.getInstance().player != null) {
                String playerName = MinecraftClient.getInstance().player.getName().getString();
                ClientPlayNetworking.send(new BroadcastPickBanPayload(playerName, goalId, "ban"));
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.0f));
            }
        }
        sendPicksBansUpdate();
    }

    private void sendPicksBansUpdate() {
        // Send picks/bans update to server to broadcast to other players
        // Build goal-to-player map for all picks and bans
        Map<String, String> goalToPlayerMap = new HashMap<>();
        for (String goalId : GoalGroup.PICKS.getGoals()) {
            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                goalToPlayerMap.put(goalId, playerName);
            }
        }
        for (String goalId : GoalGroup.BANS.getGoals()) {
            String playerName = GoalGroup.getGoalPlayer(goalId);
            if (playerName != null) {
                goalToPlayerMap.put(goalId, playerName);
            }
        }
        
        ClientPlayNetworking.send(new UpdatePicksBansPayload(
            new ArrayList<>(GoalGroup.PICKS.getGoals()),
            new ArrayList<>(GoalGroup.BANS.getGoals()),
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
            bansPanel = new DropdownGoalsPanel(
                widgetX - panelWidth - 10,
                40,
                panelWidth,
                GoalGroup.BANS.getGoals(),
                "Banned Goals"
            );
            this.addDrawableChild(bansPanel);
        }

        if (rightSpace >= 150) {
            picksPanel = new DropdownGoalsPanel(
                widgetX + widgetWidth + 10,
                40,
                panelWidth,
                GoalGroup.PICKS.getGoals(),
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

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Let the searchWidget render entries (it handles scissoring and icons)
        if (searchWidget != null) {
            searchWidget.render(context, mouseX, mouseY, delta);
        }

        // Render search text field
        if (searchTextField != null) {
            searchTextField.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

}



