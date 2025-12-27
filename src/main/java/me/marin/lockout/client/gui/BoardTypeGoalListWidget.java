package me.marin.lockout.client.gui;

import me.marin.lockout.lockout.GoalRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scrollable widget that displays all goals with checkboxes for selecting exclusions.
 */
@Environment(EnvType.CLIENT)
public class BoardTypeGoalListWidget extends ScrollableWidget {

    private static final int MARGIN_X = 5;
    private static final int ENTRY_HEIGHT = 20;
    private static final int CHECKBOX_SIZE = 12;
    private static final int CHECKBOX_MARGIN = 5;

    private final BoardTypeCreatorScreen parent;
    private final Map<String, GoalEntry> allGoals = new LinkedHashMap<>();
    private List<GoalEntry> visibleGoals = new ArrayList<>();
    private GoalEntry hoveredEntry = null;

    private int rowWidth;
    private int left;
    private int top;
    private int right;

    public BoardTypeGoalListWidget(int x, int y, int width, int height, Text message, BoardTypeCreatorScreen parent) {
        super(x, y, width, height, message);
        this.parent = parent;
        
        // Load all goal IDs from registry
        // We don't instantiate goals here to avoid errors with goals that require data
        for (String goalId : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            // Format the goal name from the ID (e.g., "minecraft:obtain_stone" -> "Obtain Stone")
            String displayName = formatGoalName(goalId);
            allGoals.put(goalId, new GoalEntry(goalId, displayName));
        }
        
        visibleGoals = new ArrayList<>(allGoals.values());
    }
    
    @SuppressWarnings("deprecation")
    private String formatGoalName(String goalId) {
        // Remove namespace prefix if present
        String name = goalId.contains(":") ? goalId.substring(goalId.indexOf(":") + 1) : goalId;
        // Replace underscores with spaces and capitalize each word
        return WordUtils.capitalizeFully(name.replace("_", " "));
    }

    @Override
    protected int getContentsHeightWithPadding() {
        return visibleGoals.size() * ENTRY_HEIGHT + 8;
    }

    @Override
    protected double getDeltaYPerScroll() {
        return ENTRY_HEIGHT;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.rowWidth = getWidth() - MARGIN_X * 2;
        this.left = getX() + MARGIN_X;
        this.top = getY();
        this.right = getX() + getWidth() - MARGIN_X;

        hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;

        context.enableScissor(this.left - 1, this.top, this.right + 1, getY() + getHeight());

        int y = 4;
        for (GoalEntry entry : visibleGoals) {
            int entryY = getY() + y - (int) getScrollY();
            
            // Only render if visible
            if (entryY + ENTRY_HEIGHT >= this.top && entryY < this.top + getHeight()) {
                entry.render(context, getX() + MARGIN_X, entryY, rowWidth, ENTRY_HEIGHT, 
                    mouseX, mouseY, Objects.equals(entry, hoveredEntry), delta);
            }
            
            y += ENTRY_HEIGHT;
        }

        context.disableScissor();
        this.drawScrollbar(context);
    }

    protected final GoalEntry getEntryAtPosition(double x, double y) {
        int relativeY = (int) (y - getY() + getScrollY() - 4);
        int index = relativeY / ENTRY_HEIGHT;
        
        if (index >= 0 && index < visibleGoals.size()) {
            return visibleGoals.get(index);
        }
        return null;
    }

    public void updateSearch(String search) {
        setScrollY(0);
        if (search.trim().isEmpty()) {
            visibleGoals = new ArrayList<>(allGoals.values());
        } else {
            String searchLower = search.toLowerCase();
            visibleGoals = allGoals.values().stream()
                .filter(entry -> entry.displayName.toLowerCase().contains(searchLower) 
                              || entry.goalId.toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredEntry != null && button == 0) { // Left click
            parent.toggleGoalExclusion(hoveredEntry.goalId);
            MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
            return true;
        }
        var bl = checkScrollbarDragged(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button) || bl;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Narration for accessibility
    }

    /**
     * Represents a single goal entry in the list
     */
    public class GoalEntry {
        private final String goalId;
        private final String displayName;

        public GoalEntry(String goalId, String goalName) {
            this.goalId = goalId;
            // Format the goal name nicely (WordUtils is deprecated but still functional)
            @SuppressWarnings("deprecation")
            String formatted = WordUtils.capitalizeFully(goalName);
            this.displayName = formatted;
        }

        public void render(DrawContext context, int x, int y, int width, int height, 
                          int mouseX, int mouseY, boolean hovered, float delta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            // Background for hovered entry
            if (hovered) {
                context.fill(x, y, x + width, y + height, 0x80FFFFFF);
            }

            // Draw checkbox
            int checkboxX = x + CHECKBOX_MARGIN;
            int checkboxY = y + (height - CHECKBOX_SIZE) / 2;
            boolean isExcluded = parent.isGoalExcluded(goalId);

            // Checkbox border
            context.fill(checkboxX, checkboxY, checkboxX + CHECKBOX_SIZE, checkboxY + CHECKBOX_SIZE, 
                hovered ? 0xFFFFFFFF : 0xFFA0A0A0);
            context.fill(checkboxX + 1, checkboxY + 1, checkboxX + CHECKBOX_SIZE - 1, checkboxY + CHECKBOX_SIZE - 1, 
                0xFF000000);

            // Checkbox fill (if excluded)
            if (isExcluded) {
                context.fill(checkboxX + 2, checkboxY + 2, checkboxX + CHECKBOX_SIZE - 2, checkboxY + CHECKBOX_SIZE - 2, 
                    0xFFFF4040);
            }

            // Draw goal name
            int textX = checkboxX + CHECKBOX_SIZE + CHECKBOX_MARGIN * 2;
            int textY = y + (height - textRenderer.fontHeight) / 2;
            
            // Truncate text if too long
            String drawText = displayName;
            int maxTextWidth = width - (CHECKBOX_SIZE + CHECKBOX_MARGIN * 3 + 5);
            if (textRenderer.getWidth(drawText) > maxTextWidth) {
                while (textRenderer.getWidth(drawText + "...") > maxTextWidth && drawText.length() > 0) {
                    drawText = drawText.substring(0, drawText.length() - 1);
                }
                drawText = drawText + "...";
            }

            context.drawText(textRenderer, drawText, textX, textY, 
                isExcluded ? 0xFFFF6060 : 0xFFFFFFFF, false);

            // Draw goal ID in smaller text if hovered
            if (hovered) {
                String idText = "[" + goalId + "]";
                int idWidth = textRenderer.getWidth(idText);
                context.drawText(textRenderer, idText, x + width - idWidth - 5, textY, 
                    0xFF808080, false);
            }
        }
    }
}
