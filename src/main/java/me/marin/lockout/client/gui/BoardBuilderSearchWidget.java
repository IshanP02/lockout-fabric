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
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.text.WordUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import me.marin.lockout.LocateData;
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
    private final Map<String, GoalEntry> registeredGoals = new LinkedHashMap<>();

    private int rowWidth;
    private int left;
    private int right;
    private int top;
    private GoalEntry hovered;
    private List<GoalEntry> visibleGoals;
    private final boolean highlightPicksBans;
    private final boolean enablePickBan;

    public BoardBuilderSearchWidget(int x, int y, int width, int height, Text text, boolean highlightPicksBans, boolean enablePickBan) {
        super(x, y, width, height, text);
        this.highlightPicksBans = highlightPicksBans;
        this.enablePickBan = enablePickBan;
        for (String id : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            registeredGoals.putIfAbsent(id, new GoalEntry(id));
        }
        visibleGoals = new ArrayList<>(registeredGoals.values());
        searchUpdated(BoardBuilderData.INSTANCE.getSearch());
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
        return visibleGoals.size() * ITEM_HEIGHT;
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
        for (GoalEntry goalEntry : visibleGoals) {
            goalEntry.render(context, idx++,getY() + y - (int)getScrollY() - 3,getX() + MARGIN_X, rowWidth - 4, 18, mouseX, mouseY, Objects.equals(goalEntry, hovered), delta);
            y += 18;
        }

        // (Buttons removed, nothing to draw here)

        context.disableScissor();
        this.drawScrollbar(context);
    }

    protected final GoalEntry getEntryAtPosition(double x, double y) {
        int halfRowWidth = this.rowWidth / 2;
        int centerX = this.left + this.width / 2;
        int left = centerX - halfRowWidth;
        int right = centerX + halfRowWidth;
        int scrolledY = MathHelper.floor(y - (double)this.top) + (int)getScrollY() - MARGIN_Y + 3;
        int idx = scrolledY / ITEM_HEIGHT;
        if (x < (this.right + MARGIN_X - 6) && x >= (double) left && x <= (double) right && idx >= 0 && scrolledY >= 0 && idx < visibleGoals.size()) {
            return registeredGoals.get(visibleGoals.get(idx).goal.getId());
        }
        return null;
    }

    /**
     * Returns the goal id at the given screen coordinates, or null if none.
     */
    public String getGoalIdAtPosition(double x, double y) {
        GoalEntry entry = this.getEntryAtPosition(x, y);
        return entry == null ? null : entry.goal.getId();
    }

    public void searchUpdated(String search) {
        setScrollY(0);
        visibleGoals = new ArrayList<>(registeredGoals.values()).stream().filter(goalEntry -> goalEntry.displayName.toLowerCase().contains(search.toLowerCase())).collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered != null && enablePickBan) {
            List<String> picks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
            List<String> bans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();
            String goalId = hovered.goal.getId();
            if (button == 0) { // Left click: toggle pick
                if (picks.contains(goalId)) {
                    picks.remove(goalId);
                } else {
                    picks.add(goalId);
                    bans.remove(goalId);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Added to Picks!"), false);
                }
            } else if (button == 1) { // Right click: toggle ban
                if (bans.contains(goalId)) {
                    bans.remove(goalId);
                } else {
                    bans.add(goalId);
                    picks.remove(goalId);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Added to Bans!"), false);
                }
            }
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        if (hovered != null && !enablePickBan) {
            BoardBuilderData.INSTANCE.setGoal(hovered.goal);
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        var bl = checkScrollbarDragged(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button) || bl;
    }

    // Helper to get the Y position for the buttons for the selected goal
    private int getButtonYForSelected() {
        if (selectedGoalId == null) return 0;
        int idx = -1;
        for (int i = 0; i < visibleGoals.size(); i++) {
            if (visibleGoals.get(i).goal.getId().equals(selectedGoalId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return 0;
        int y = getY() + 4 + idx * ITEM_HEIGHT - (int)getScrollY() - 3 + ITEM_HEIGHT + 2;
        return y;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public final class GoalEntry extends AlwaysSelectedEntryListWidget.Entry<GoalEntry> {

        private final Goal goal;
        public final String displayName;

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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            // Highlight picked/banned goals only if enabled
            if (highlightPicksBans) {
                List<String> picks = me.marin.lockout.generator.GoalGroup.PICKS.getGoals();
                List<String> bans = me.marin.lockout.generator.GoalGroup.BANS.getGoals();
                int highlightColor = 0;
                if (picks.contains(goal.getId())) {
                    highlightColor = 0x5500FF55; // semi-transparent green
                } else if (bans.contains(goal.getId())) {
                    highlightColor = 0x55FF5555; // semi-transparent red
                }
                if (highlightColor != 0) {
                    context.fill(x - 1, y, x + entryWidth + 2, y + entryHeight - 1, highlightColor);
                }
            }

            goal.render(context, textRenderer, x, y);
            context.drawTextWithShadow(textRenderer, displayName, x + 18, y + 5, Color.WHITE.getRGB());
            if (hovered) {
                context.drawBorder(x - 1, y - 1, entryWidth + 2, entryHeight, Color.LIGHT_GRAY.getRGB());
            }
        }
    }


}
