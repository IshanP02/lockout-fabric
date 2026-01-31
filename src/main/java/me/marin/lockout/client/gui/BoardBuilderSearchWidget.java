package me.marin.lockout.client.gui;

import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import me.marin.lockout.Lockout;
import me.marin.lockout.LocateData;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.generator.GoalRequirements;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

@Environment(EnvType.CLIENT)
public class BoardBuilderSearchWidget extends ScrollableWidget {

    // Track the currently selected goal id (null if none)
    private String selectedGoalId = null;

    private static final int MARGIN_X = 3;
    private static final int MARGIN_Y = 3;
    private static final int ITEM_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 16;
    private final Map<String, GoalEntry> registeredGoals = new LinkedHashMap<>();

    private int rowWidth;
    private int left;
    private int right;
    private int top;
    private Entry hovered;
    private List<Entry> visibleEntries;
    private final boolean highlightPicksBans;
    private final boolean enablePickBan;

    public BoardBuilderSearchWidget(int x, int y, int width, int height, Text text, boolean highlightPicksBans, boolean enablePickBan, String initialSearch) {
        super(x, y, width, height, text);
        this.highlightPicksBans = highlightPicksBans;
        this.enablePickBan = enablePickBan;
        for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            registeredGoals.putIfAbsent(id, new GoalEntry(id));
        }
        visibleEntries = buildEntriesWithHeaders(new ArrayList<>(registeredGoals.values()));
        
        searchUpdated(initialSearch != null ? initialSearch : "");
    }

    public void filterByRequirements(Map<RegistryKey<Biome>, LocateData> biomes, Map<RegistryKey<Structure>, LocateData> structures) {
        // Remove any registered goals whose GoalRequirements are not satisfied by the provided locate maps
        List<String> toRemove = new ArrayList<>();
        for (String id : new ArrayList<>(registeredGoals.keySet())) {
            GoalRequirements req = GoalRegistry.INSTANCE.getGoalGenerator(id);
            if (req == null) continue;
            if (!req.isSatisfied(biomes == null ? Collections.emptyMap() : biomes, structures == null ? Collections.emptyMap() : structures)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            registeredGoals.remove(id);
        }

        // Refresh visible list
        searchUpdated(BoardBuilderData.INSTANCE.getSearch());
    }

    public void setScrollY(double scrollY) {
        super.setScrollY(scrollY);
    }

    public double getScrollY() {
        return super.getScrollY();
    }

    @Override
    protected int getContentsHeightWithPadding() {
        int height = 0;
        for (Entry entry : visibleEntries) {
            height += entry.getHeight();
        }
        return height;
    }

    @Override
    protected double getDeltaYPerScroll() {
        return ITEM_HEIGHT / 2.0;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.rowWidth = getWidth() - MARGIN_X * 2;
        this.left = getX() + MARGIN_X;
        this.top = getY();
        this.right = getX() + getWidth() - MARGIN_X;

        hovered = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;

        context.enableScissor(this.left - 1, this.top, this.right + 1, getY() + getHeight());
        

        int y = 4;
        int idx = 0;
        for (Entry entry : visibleEntries) {
            int entryHeight = entry.getHeight();
            int entryY = getY() + y - (int)getScrollY() - 3;
            
            // (debug overlays removed)
            entry.render(context, idx++, entryY, Objects.equals(entry, hovered), delta);
            y += entryHeight;
        }

        // (Buttons removed, nothing to draw here)

        context.disableScissor();
        this.drawScrollbar(context, mouseX, mouseY);
    }

    protected final Entry getEntryAtPosition(double x, double y) {
        int halfRowWidth = this.rowWidth / 2;
        int centerX = this.left + this.width / 2;
        int left = centerX - halfRowWidth;
        int right = centerX + halfRowWidth;
        int scrolledY = MathHelper.floor(y - (double)this.top) + (int)getScrollY() - MARGIN_Y + 3;
        
        int currentY = 0;
        for (Entry entry : visibleEntries) {
            int entryHeight = entry.getHeight();
            if (scrolledY >= currentY && scrolledY < currentY + entryHeight) {
                if (x < (this.right + MARGIN_X - 6) && x >= (double) left && x <= (double) right) {
                    return entry;
                }
                break;
            }
            currentY += entryHeight;
        }
        return null;
    }

    /**
     * Returns the goal id at the given screen coordinates, or null if none.
     */
    public String getGoalIdAtPosition(double x, double y) {
        Entry entry = this.getEntryAtPosition(x, y);
        if (entry instanceof GoalEntry goalEntry) {
            return goalEntry.goal.getId();
        }
        return null;
    }

    public void searchUpdated(String search) {
        setScrollY(0);
        
        List<GoalEntry> filteredGoals = new ArrayList<>(registeredGoals.values()).stream()
            .filter(goalEntry -> goalEntry.displayName.toLowerCase().contains(search.toLowerCase()))
            .collect(Collectors.toList());
        visibleEntries = buildEntriesWithHeaders(filteredGoals);
        String sample = filteredGoals.stream().limit(5).map(g -> g.displayName).collect(Collectors.joining(", "));
        
    }
    
    /**
     * Refresh the widget when board type changes
     */
    public void refreshFromBoardType() {
        // Just trigger a re-render, the excluded goals will be grayed out in render()
        // No need to rebuild entries since we want to show all goals, just gray out excluded ones
    }
    
    /**
     * Check if a goal is excluded by the current board type
     */
    private boolean isExcludedByBoardType(String goalId) {
        if (!enablePickBan) return false; // Only check in PickBan GUI
        return LockoutClient.currentExcludedGoals.contains(goalId);
    }
    
    /**
     * Refresh the goal list from the registry (used when board type changes)
     */
    public void refreshGoals() {
        registeredGoals.clear();
        for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            registeredGoals.putIfAbsent(id, new GoalEntry(id));
        }
        visibleEntries = buildEntriesWithHeaders(new ArrayList<>(registeredGoals.values()));
        
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int button = click.button();
        double mouseX = click.x();
        double mouseY = click.y();
        if (hovered instanceof GoalEntry hoveredGoal && enablePickBan) {
            String goalId = hoveredGoal.goal.getId();
            
            // Block interaction if goal is excluded by board type
            if (isExcludedByBoardType(goalId)) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("This goal is excluded by the current board type!").withColor(0xFF5555),
                    false
                );
                return true; // Block the click
            }
            
            me.marin.lockout.network.UpdatePickBanSessionPayload session = me.marin.lockout.client.ClientPickBanSessionHolder.getActiveSession();

            // During a session, use PENDING lists for temporary selections; otherwise use PICKS/BANS
            List<String> picks = session != null
                ? me.marin.lockout.generator.GoalGroup.PENDING_PICKS.getGoals()
                : me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> bans = session != null
                ? me.marin.lockout.generator.GoalGroup.PENDING_BANS.getGoals()
                : me.marin.lockout.generator.GoalGroup.BANS.getGoals();
            
            // Check if goal is locked in PICKS or BANS (all locked goals from all teams)
            List<String> lockedPicks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> lockedBans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();
            
            if (lockedPicks.contains(goalId) || lockedBans.contains(goalId)) {
                String message = session != null 
                    ? "This goal has already been locked by a team!"
                    : "This goal is locked. Use /RemovePicks or /RemoveBans to unlock it.";
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal(message).withColor(0xFF5555),
                    false
                );
                return true;
            }
            
            if (session != null) {
                // Check if player is on the active team FIRST - this blocks ANY interaction
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) {
                    return true; // Block if no player
                }
                
                String activeTeamName = session.isTeam1Turn() ? session.team1Name() : session.team2Name();
                
                // Get the player's team from scoreboard
                net.minecraft.scoreboard.Team playerTeam = client.player.getScoreboardTeam();
                if (playerTeam == null) {
                    return true; // Block the interaction
                }
                
                if (!playerTeam.getName().equals(activeTeamName)) {
                    return true; // Block the interaction
                }
            }
            
            if (button == 0) { // Left click: toggle pick
                // Block picks during ban rounds (odd rounds)
                if (session != null) {
                    int currentRound = session.currentRound();
                    if (currentRound % 2 == 1) { // Ban round
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("You can only BAN goals during this round! Right-click to ban.").withColor(0xFF5555),
                            false
                        );
                        return true;
                    }
                }
                
                // Check if at pick limit during session
                if (session != null && !picks.contains(goalId)) {
                    if (picks.size() >= session.selectionLimit()) {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("You've reached the maximum number of picks (" + session.selectionLimit() + ")").withColor(0xFF5555),
                            false
                        );
                        return true;
                    }
                }
                
                if (picks.contains(goalId)) {
                    picks.remove(goalId);
                } else {
                    picks.add(goalId);
                    bans.remove(goalId);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Added to Picks!"), false);
                }
            } else if (button == 1) { // Right click: toggle ban
                // Block bans during pick rounds (even rounds)
                if (session != null) {
                    int currentRound = session.currentRound();
                    if (currentRound % 2 == 0) { // Pick round
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("You can only PICK goals during this round! Left-click to pick.").withColor(0xFF5555),
                            false
                        );
                        return true;
                    }
                }
                
                // Check if at ban limit during session
                if (session != null && !bans.contains(goalId)) {
                    if (bans.size() >= session.selectionLimit()) {
                        MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("You've reached the maximum number of bans (" + session.selectionLimit() + ")").withColor(0xFF5555),
                            false
                        );
                        return true;
                    }
                }
                
                if (bans.contains(goalId)) {
                    bans.remove(goalId);
                } else {
                    bans.add(goalId);
                    picks.remove(goalId);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Added to Bans!"), false);
                }
            }
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (hovered instanceof GoalEntry hoveredGoal2 && !enablePickBan) {
            BoardBuilderData.INSTANCE.setGoal(hoveredGoal2.goal);
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        var bl = checkScrollbarDragged(click);
        return super.mouseClicked(click, doubleClick) || bl;
    }

    // Helper to get the Y position for the buttons for the selected goal
    private int getButtonYForSelected() {
        if (selectedGoalId == null) return 0;
        int currentY = 0;
        for (Entry entry : visibleEntries) {
            if (entry instanceof GoalEntry goalEntry && goalEntry.goal.getId().equals(selectedGoalId)) {
                int y = getY() + 4 + currentY - (int)getScrollY() - 3 + entry.getHeight() + 2;
                return y;
            }
            currentY += entry.getHeight();
        }
        return 0;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    private List<Entry> buildEntriesWithHeaders(List<GoalEntry> goals) {
        List<Entry> entries = new ArrayList<>();
        
        // Group goals by category
        Map<String, List<GoalEntry>> grouped = new LinkedHashMap<>();
        for (GoalEntry goal : goals) {
            String category = getCategoryForGoal(goal.goal.getId());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(goal);
        }
        
        // Add headers and goals
        for (Map.Entry<String, List<GoalEntry>> group : grouped.entrySet()) {
            entries.add(new HeaderEntry(group.getKey()));
            entries.addAll(group.getValue());
        }
        
        return entries;
    }
    
    private String getCategoryForGoal(String goalId) {
        // Map goals to categories based on CATEGORY GoalGroup membership
        if (me.marin.lockout.generator.GoalGroup.TOOLS_CATEGORY.getGoals().contains(goalId)) return "Use Tools";
        if (me.marin.lockout.generator.GoalGroup.ARMOR_CATEGORY.getGoals().contains(goalId)) return "Wear Armor";
        if (me.marin.lockout.generator.GoalGroup.TAME_CATEGORY.getGoals().contains(goalId)) return "Tame Animal";
        if (me.marin.lockout.generator.GoalGroup.RIDE_CATEGORY.getGoals().contains(goalId)) return "Ride Entity";
        if (me.marin.lockout.generator.GoalGroup.BIOMES_CATEGORY.getGoals().contains(goalId)) return "Visit Biome";
        if (me.marin.lockout.generator.GoalGroup.BREED_CATEGORY.getGoals().contains(goalId)) return "Breed Animals";
        if (me.marin.lockout.generator.GoalGroup.LEASH_CATEGORY.getGoals().contains(goalId)) return "Leash Entities";
        if (me.marin.lockout.generator.GoalGroup.SPYGLASS_CATEGORY.getGoals().contains(goalId)) return "Spyglass";
        if (me.marin.lockout.generator.GoalGroup.BREWING_CATEGORY.getGoals().contains(goalId)) return "Brewing";
        if (me.marin.lockout.generator.GoalGroup.CONSUME_CATEGORY.getGoals().contains(goalId)) return "Consume";
        if (me.marin.lockout.generator.GoalGroup.DEATH_CATEGORY.getGoals().contains(goalId)) return "Death Tasks";
        if (me.marin.lockout.generator.GoalGroup.KILL_CATEGORY.getGoals().contains(goalId)) return "Kill Entity";
        if (me.marin.lockout.generator.GoalGroup.MINE_CATEGORY.getGoals().contains(goalId)) return "Mine Block";
        if (me.marin.lockout.generator.GoalGroup.ENTER_STRUCTURE_DIMENSION_CATEGORY.getGoals().contains(goalId)) return "Enter Dimension/Structure";
        if (me.marin.lockout.generator.GoalGroup.OPPONENT_DOES_X_CATEGORY.getGoals().contains(goalId)) return "Opponent Does Action";
        if (me.marin.lockout.generator.GoalGroup.WORKSTATION_CATEGORY.getGoals().contains(goalId)) return "Workstation";
        if (me.marin.lockout.generator.GoalGroup.FILL_INTERFACE_CATEGORY.getGoals().contains(goalId)) return "Fill Interface";
        if (me.marin.lockout.generator.GoalGroup.OBTAIN_CATEGORY.getGoals().contains(goalId)) return "Obtain Item";
        if (me.marin.lockout.generator.GoalGroup.EXPERIENCE_CATEGORY.getGoals().contains(goalId)) return "Experience";
        if (me.marin.lockout.generator.GoalGroup.HAVE_MORE_CATEGORY.getGoals().contains(goalId)) return "Have More";
        if (me.marin.lockout.generator.GoalGroup.ADVANCEMENTS_CATEGORY.getGoals().contains(goalId)) return "Advancements";
        if (me.marin.lockout.generator.GoalGroup.STATUS_EFFECT_CATEGORY.getGoals().contains(goalId)) return "Status Effects";
        if (me.marin.lockout.generator.GoalGroup.STATISTICS_CATEGORY.getGoals().contains(goalId)) return "Statistics";
        if (me.marin.lockout.generator.GoalGroup.MISCELLANEOUS_CATEGORY.getGoals().contains(goalId)) return "Miscellaneous";
        return "Uncategorized";
    }

    public abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        public abstract int getHeight();
        
        @Override
        public Text getNarration() {
            return Text.empty();
        }
        
        @Override
        public abstract void render(DrawContext context, int index, int y, boolean hovered, float tickDelta);
    }
    
    public final class HeaderEntry extends Entry {
        private final String categoryName;
        
        public HeaderEntry(String categoryName) {
            this.categoryName = categoryName;
        }
        
        @Override
        public int getHeight() {
            return HEADER_HEIGHT;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, boolean hovered, float tickDelta) {
            int x = BoardBuilderSearchWidget.this.left;
            int entryWidth = rowWidth - 4;
            int entryHeight = getHeight();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            // Draw a subtle background
            context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight, 0x66000000);

            // Draw category name bold and centered in gold/yellow
            Text boldText = Text.literal(categoryName);
            int textWidth = textRenderer.getWidth(boldText);
            int centerX = x + (entryWidth / 2) - (textWidth / 2);
            context.drawText(textRenderer, boldText, centerX, y + 4, 0xFFFFAA00, true);

            // Draw a line underneath
            context.fill(x, y + entryHeight - 1, x + entryWidth, y + entryHeight, 0xFFFFAA00);
        }
    }

    public final class GoalEntry extends Entry {

        private final Goal goal;
        public final String displayName;
        
        @Override
        public int getHeight() {
            return ITEM_HEIGHT;
        }

        public GoalEntry(String id) {
            Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(id);

            // generate random data
            String data = gen.map(g -> g.generateData(new ArrayList<>(GoalDataGenerator.ALL_DYES))).orElse(GoalDataConstants.DATA_NONE);
            this.goal = GoalRegistry.INSTANCE.newGoal(id, data);

            this.displayName = gen.isEmpty() ? goal.getGoalName() : "[*] " + WordUtils.capitalize(goal.getId().replace("_", " ").toLowerCase(), ' ');
        }


        @Override
        public Text getNarration() {
            return Text.empty();
        }

        @Override
        public void render(DrawContext context, int index, int y, boolean hovered, float tickDelta) {
            int x = BoardBuilderSearchWidget.this.left;
            int entryWidth = rowWidth - 4;
            int entryHeight = getHeight();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            // Check if goal is locked (in PICKS or BANS, but not PENDING)
            List<String> picks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> bans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();
            List<String> pendingPicks = me.marin.lockout.generator.GoalGroup.PENDING_PICKS.getGoals();
            List<String> pendingBans = me.marin.lockout.generator.GoalGroup.PENDING_BANS.getGoals();
            
            boolean isLocked = picks.contains(goal.getId()) || bans.contains(goal.getId());
            boolean isExcluded = isExcludedByBoardType(goal.getId());
            
            // Highlight picked/banned goals only if enabled
            if (highlightPicksBans) {
                int highlightColor = 0;
                if (picks.contains(goal.getId()) || pendingPicks.contains(goal.getId())) {
                    highlightColor = 0x5500FF55; // semi-transparent green
                } else if (bans.contains(goal.getId()) || pendingBans.contains(goal.getId())) {
                    highlightColor = 0x55FF5555; // semi-transparent red
                }
                if (highlightColor != 0) {
                    context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight - 1, highlightColor);
                }
            }

            goal.render(context, textRenderer, x, y);
            context.drawText(textRenderer, displayName, x + 18, y + 5, Color.WHITE.getRGB(), true);
            
            // Gray out locked goals or board type excluded goals (only in pick/ban GUI)
            if (enablePickBan && (isLocked || isExcluded)) {
                context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight - 1, 0x88000000); // semi-transparent black overlay
            }
            
                // Only show hover border if goal is not excluded
                if (hovered && !isExcluded) {
                    context.fill(x - 2, y - 2, x + entryWidth + 3, y - 1, Color.LIGHT_GRAY.getRGB());
                    context.fill(x - 2, y + entryHeight, x + entryWidth + 3, y + entryHeight + 1, Color.LIGHT_GRAY.getRGB());
                    context.fill(x - 2, y - 1, x - 1, y + entryHeight, Color.LIGHT_GRAY.getRGB());
                    context.fill(x + entryWidth + 2, y - 1, x + entryWidth + 3, y + entryHeight, Color.LIGHT_GRAY.getRGB());
                }
        }
    }


}
