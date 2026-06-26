package me.marin.lockout.client.gui;

import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import me.marin.lockout.LocateData;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.generator.GoalRequirements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

@Environment(EnvType.CLIENT)
public class BoardBuilderSearchWidget extends AbstractScrollArea {

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

    public BoardBuilderSearchWidget(int x, int y, int width, int height, Component text, boolean highlightPicksBans, boolean enablePickBan, String initialSearch) {
        super(x, y, width, height, text, AbstractScrollArea.defaultSettings(ITEM_HEIGHT));
        this.highlightPicksBans = highlightPicksBans;
        this.enablePickBan = enablePickBan;
        for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            registeredGoals.putIfAbsent(id, new GoalEntry(id));
        }
        visibleEntries = buildEntriesWithHeaders(new ArrayList<>(registeredGoals.values()));
        searchUpdated(initialSearch != null ? initialSearch : "");
    }

    public void filterByRequirements(Map<ResourceKey<Biome>, LocateData> biomes, Map<ResourceKey<Structure>, LocateData> structures) {
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
        searchUpdated(BoardBuilderData.INSTANCE.getSearch());
    }

    public void setScrollY(double scrollY) {
        this.setScrollAmount(scrollY);
    }

    public double getScrollY() {
        return this.scrollAmount();
    }

    @Override
    protected int contentHeight() {
        int height = 0;
        for (Entry entry : visibleEntries) {
            height += entry.getHeight();
        }
        return height;
    }

    @Override
    protected double scrollRate() {
        return ITEM_HEIGHT / 2.0;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
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
            int entryY = getY() + y - (int) scrollAmount() - 3;
            entry.render(context, idx++, entryY, Objects.equals(entry, hovered), delta);
            y += entryHeight;
        }

        context.disableScissor();
        this.extractScrollbar(context, mouseX, mouseY);
    }

    protected final Entry getEntryAtPosition(double x, double y) {
        int halfRowWidth = this.rowWidth / 2;
        int centerX = this.left + this.width / 2;
        int left = centerX - halfRowWidth;
        int right = centerX + halfRowWidth;
        int scrolledY = Mth.floor(y - (double) this.top) + (int) scrollAmount() - MARGIN_Y + 3;

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

    public String getGoalIdAtPosition(double x, double y) {
        Entry entry = this.getEntryAtPosition(x, y);
        if (entry instanceof GoalEntry goalEntry) {
            return goalEntry.goal.getId();
        }
        return null;
    }

    public void searchUpdated(String search) {
        setScrollAmount(0);
        List<GoalEntry> filteredGoals = new ArrayList<>(registeredGoals.values()).stream()
            .filter(goalEntry -> goalEntry.displayName.toLowerCase().contains(search.toLowerCase()))
            .collect(Collectors.toList());
        visibleEntries = buildEntriesWithHeaders(filteredGoals);
    }

    public void refreshFromBoardType() {}

    private boolean isExcludedByBoardType(String goalId) {
        if (!enablePickBan) return false;
        return LockoutClient.currentExcludedGoals.contains(goalId);
    }

    public void refreshGoals() {
        registeredGoals.clear();
        for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            registeredGoals.putIfAbsent(id, new GoalEntry(id));
        }
        visibleEntries = buildEntriesWithHeaders(new ArrayList<>(registeredGoals.values()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        int button = click.button();
        if (hovered instanceof GoalEntry hoveredGoal && enablePickBan) {
            String goalId = hoveredGoal.goal.getId();

            if (isExcludedByBoardType(goalId)) {
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("This goal is excluded by the current board type!").withColor(0xFF5555));
                return true;
            }

            me.marin.lockout.network.UpdatePickBanSessionPayload session = me.marin.lockout.client.ClientPickBanSessionHolder.getActiveSession();

            List<String> picks = session != null
                ? me.marin.lockout.generator.GoalGroup.PENDING_PICKS.getGoals()
                : me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> bans = session != null
                ? me.marin.lockout.generator.GoalGroup.PENDING_BANS.getGoals()
                : me.marin.lockout.generator.GoalGroup.BANS.getGoals();

            List<String> lockedPicks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> lockedBans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();

            if (lockedPicks.contains(goalId) || lockedBans.contains(goalId)) {
                String message = session != null
                    ? "This goal has already been locked by a team!"
                    : "This goal is locked. Use /RemovePicks or /RemoveBans to unlock it.";
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(message).withColor(0xFF5555));
                return true;
            }

            if (session != null) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return true;

                String activeTeamName = session.isTeam1Turn() ? session.team1Name() : session.team2Name();
                net.minecraft.world.scores.Team playerTeam = client.player.getTeam();
                if (playerTeam == null) return true;
                if (!playerTeam.getName().equals(activeTeamName)) return true;
            }

            if (button == 0) {
                if (session != null) {
                    int currentRound = session.currentRound();
                    if (currentRound % 2 == 1) {
                        Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("You can only BAN goals during this round! Right-click to ban.").withColor(0xFF5555));
                        return true;
                    }
                }
                if (session != null && !picks.contains(goalId) && picks.size() >= session.selectionLimit()) {
                    Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("You've reached the maximum number of picks (" + session.selectionLimit() + ")").withColor(0xFF5555));
                    return true;
                }
                if (picks.contains(goalId)) {
                    picks.remove(goalId);
                } else {
                    picks.add(goalId);
                    bans.remove(goalId);
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("Added to Picks!"));
                }
            } else if (button == 1) {
                if (session != null) {
                    int currentRound = session.currentRound();
                    if (currentRound % 2 == 0) {
                        Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("You can only PICK goals during this round! Left-click to pick.").withColor(0xFF5555));
                        return true;
                    }
                }
                if (session != null && !bans.contains(goalId) && bans.size() >= session.selectionLimit()) {
                    Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("You've reached the maximum number of bans (" + session.selectionLimit() + ")").withColor(0xFF5555));
                    return true;
                }
                if (bans.contains(goalId)) {
                    bans.remove(goalId);
                } else {
                    bans.add(goalId);
                    picks.remove(goalId);
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("Added to Bans!"));
                }
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (hovered instanceof GoalEntry hoveredGoal2 && !enablePickBan) {
            BoardBuilderData.INSTANCE.setGoal(hoveredGoal2.goal);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        boolean bl = updateScrolling(click);
        return super.mouseClicked(click, doubleClick) || bl;
    }

    private int getButtonYForSelected() {
        if (selectedGoalId == null) return 0;
        int currentY = 0;
        for (Entry entry : visibleEntries) {
            if (entry instanceof GoalEntry goalEntry && goalEntry.goal.getId().equals(selectedGoalId)) {
                return getY() + 4 + currentY - (int) scrollAmount() - 3 + entry.getHeight() + 2;
            }
            currentY += entry.getHeight();
        }
        return 0;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    private List<Entry> buildEntriesWithHeaders(List<GoalEntry> goals) {
        List<Entry> entries = new ArrayList<>();
        Map<String, List<GoalEntry>> grouped = new LinkedHashMap<>();
        for (GoalEntry goal : goals) {
            String category = getCategoryForGoal(goal.goal.getId());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(goal);
        }
        for (Map.Entry<String, List<GoalEntry>> group : grouped.entrySet()) {
            entries.add(new HeaderEntry(group.getKey()));
            entries.addAll(group.getValue());
        }
        return entries;
    }

    private String getCategoryForGoal(String goalId) {
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

    public abstract class Entry {
        public abstract int getHeight();
        public abstract void render(GuiGraphicsExtractor context, int index, int y, boolean hovered, float tickDelta);
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
        public void render(GuiGraphicsExtractor context, int index, int y, boolean hovered, float tickDelta) {
            int x = BoardBuilderSearchWidget.this.left;
            int entryWidth = rowWidth - 4;
            int entryHeight = getHeight();
            Font font = Minecraft.getInstance().font;

            context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight, 0x66000000);

            Component boldText = Component.literal(categoryName);
            int textWidth = font.width(boldText);
            int centerX = x + (entryWidth / 2) - (textWidth / 2);
            context.text(font, boldText, centerX, y + 4, 0xFFFFAA00, true);

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
            String data = gen.map(g -> g.generateData(new ArrayList<>(GoalDataGenerator.ALL_DYES))).orElse(GoalDataConstants.DATA_NONE);
            this.goal = GoalRegistry.INSTANCE.newGoal(id, data);
            this.displayName = gen.isEmpty() ? goal.getGoalName() : "[*] " + WordUtils.capitalize(goal.getId().replace("_", " ").toLowerCase(), ' ');
        }

        @Override
        public void render(GuiGraphicsExtractor context, int index, int y, boolean hovered, float tickDelta) {
            int x = BoardBuilderSearchWidget.this.left;
            int entryWidth = rowWidth - 4;
            int entryHeight = getHeight();
            Font font = Minecraft.getInstance().font;

            List<String> picks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> bans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();
            List<String> pendingPicks = me.marin.lockout.generator.GoalGroup.PENDING_PICKS.getGoals();
            List<String> pendingBans = me.marin.lockout.generator.GoalGroup.PENDING_BANS.getGoals();

            boolean isLocked = picks.contains(goal.getId()) || bans.contains(goal.getId());
            boolean isExcluded = isExcludedByBoardType(goal.getId());

            if (highlightPicksBans) {
                int highlightColor = 0;
                if (picks.contains(goal.getId()) || pendingPicks.contains(goal.getId())) {
                    highlightColor = 0x5500FF55;
                } else if (bans.contains(goal.getId()) || pendingBans.contains(goal.getId())) {
                    highlightColor = 0x55FF5555;
                }
                if (highlightColor != 0) {
                    context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight - 1, highlightColor);
                }
            }

            goal.render(context, font, x, y);
            context.text(font, displayName, x + 18, y + 5, Color.WHITE.getRGB(), true);

            if (enablePickBan && (isLocked || isExcluded)) {
                context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight - 1, 0x88000000);
            }

            if (hovered && !isExcluded) {
                context.fill(x - 2, y - 2, x + entryWidth + 3, y - 1, Color.LIGHT_GRAY.getRGB());
                context.fill(x - 2, y + entryHeight, x + entryWidth + 3, y + entryHeight + 1, Color.LIGHT_GRAY.getRGB());
                context.fill(x - 2, y - 1, x - 1, y + entryHeight, Color.LIGHT_GRAY.getRGB());
                context.fill(x + entryWidth + 2, y - 1, x + entryWidth + 3, y + entryHeight, Color.LIGHT_GRAY.getRGB());
            }
        }
    }
}
