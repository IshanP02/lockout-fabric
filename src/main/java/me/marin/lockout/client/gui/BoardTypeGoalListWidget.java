package me.marin.lockout.client.gui;

import me.marin.lockout.generator.GoalDataGenerator;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.GoalRegistry;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.BreedAnimalGoal;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.interfaces.TameAnimalGoal;
import me.marin.lockout.lockout.texture.CycleItemTexturesProvider;
import me.marin.lockout.lockout.texture.CycleTexturesProvider;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scrollable widget that displays all goals with icons for selecting exclusions.
 */
@Environment(EnvType.CLIENT)
public class BoardTypeGoalListWidget extends ScrollableWidget {

    private static final int MARGIN_X = 5;
    private static final int ENTRY_HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_MARGIN = 2;

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
        
        for (String goalId : GoalRegistry.INSTANCE.getRegisteredGoals()) {
            Goal goal = null;
            try {
                Optional<GoalDataGenerator> gen = GoalRegistry.INSTANCE.getDataGenerator(goalId);
                String data = gen.map(g -> g.generateData(new ArrayList<>(GoalDataGenerator.ALL_DYES)))
                    .orElse(GoalDataConstants.DATA_NONE);
                goal = GoalRegistry.INSTANCE.newGoal(goalId, data);
            } catch (Exception e) {
                // Goal instantiation failed, will show without icon
            }
            
            String displayName = goal != null ? goal.getGoalName() : formatGoalName(goalId);
            
            ItemStack icon = null;
            if (goal != null) {
                icon = goal.getTextureItemStack();
                if (icon == null && goal instanceof CycleItemTexturesProvider cycleProvider) {
                    List<net.minecraft.item.Item> items = cycleProvider.getItemsToDisplay();
                    if (items != null && !items.isEmpty()) {
                        icon = items.get(0).getDefaultStack();
                    }
                }
                if (icon == null) {
                    icon = getEntitySpawnEggIcon(goal);
                }
                if (icon == null && (goal instanceof TextureProvider || goal instanceof CycleTexturesProvider)) {
                    icon = getFallbackIconForGoal(goalId);
                }
            }
            
            allGoals.put(goalId, new GoalEntry(goalId, displayName, icon));
        }
        
        visibleGoals = new ArrayList<>(allGoals.values());
    }
    
    private ItemStack getEntitySpawnEggIcon(Goal goal) {
        EntityType<?> entityType = null;
        
        if (goal instanceof KillMobGoal killGoal) {
            entityType = killGoal.getEntity();
        } else if (goal instanceof BreedAnimalGoal breedGoal) {
            entityType = breedGoal.getAnimal();
        } else if (goal instanceof TameAnimalGoal tameGoal) {
            entityType = tameGoal.getAnimal();
        }
        
        if (entityType != null) {
            SpawnEggItem spawnEgg = SpawnEggItem.forEntity(entityType);
            if (spawnEgg != null) {
                return spawnEgg.getDefaultStack();
            }
        }
        
        return null;
    }
    
    private ItemStack getFallbackIconForGoal(String goalId) {
        String lowerGoalId = goalId.toLowerCase();
        
        if (lowerGoalId.contains("kill")) {
            return Items.WOODEN_SWORD.getDefaultStack();
        }
        if (lowerGoalId.contains("die")) {
            return Items.SKELETON_SKULL.getDefaultStack();
        }
        if (lowerGoalId.contains("breed")) {
            return Items.WHEAT.getDefaultStack();
        }
        if (lowerGoalId.contains("tame")) {
            return Items.BONE.getDefaultStack();
        }
        
        return Items.PAPER.getDefaultStack();
    }
    
    @SuppressWarnings("deprecation")
    private String formatGoalName(String goalId) {
        String name = goalId.contains(":") ? goalId.substring(goalId.indexOf(":") + 1) : goalId;
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
        if (hoveredEntry != null && button == 0 && !checkScrollbarDragged(mouseX, mouseY, button)) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrollbarDragged) {
            int top = this.getY();
            int bottom = top + this.getHeight();
            int scrollbarHeight = this.getScrollbarThumbHeight();
            double normalizedPos = (mouseY - top - (double) scrollbarHeight / 2.0) / (double) (bottom - top - scrollbarHeight);
            normalizedPos = Math.max(0.0, Math.min(1.0, normalizedPos));
            this.setScrollY(normalizedPos * (double) this.getMaxScrollY());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrollbarDragged = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public class GoalEntry {
        private final String goalId;
        private final String displayName;
        private final ItemStack icon;

        public GoalEntry(String goalId, String goalName, ItemStack icon) {
            this.goalId = goalId;
            this.displayName = goalName;
            this.icon = icon;
        }

        public void render(DrawContext context, int x, int y, int width, int height, 
                          int mouseX, int mouseY, boolean hovered, float delta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            boolean isExcluded = parent.isGoalExcluded(goalId);

            if (isExcluded) {
                context.fill(x, y, x + width, y + height, 0x80FF4040);
            } else if (hovered) {
                context.fill(x, y, x + width, y + height, 0x80FFFFFF);
            }

            int iconX = x + ICON_MARGIN;
            int iconY = y + (height - ICON_SIZE) / 2;
            if (icon != null) {
                context.drawItem(icon, iconX, iconY);
            }

            int textX = iconX + ICON_SIZE + ICON_MARGIN * 2;
            int textY = y + (height - textRenderer.fontHeight) / 2;
            
            String drawText = displayName;
            int maxTextWidth = width - (ICON_SIZE + ICON_MARGIN * 3 + 5);
            if (textRenderer.getWidth(drawText) > maxTextWidth) {
                while (textRenderer.getWidth(drawText + "...") > maxTextWidth && drawText.length() > 0) {
                    drawText = drawText.substring(0, drawText.length() - 1);
                }
                drawText = drawText + "...";
            }

            context.drawText(textRenderer, drawText, textX, textY, 0xFFFFFFFF, false);

            if (hovered) {
                String idText = "[" + goalId + "]";
                int idWidth = textRenderer.getWidth(idText);
                context.drawText(textRenderer, idText, x + width - idWidth - 5, textY, 
                    0xFF808080, false);
            }
        }
    }
}
