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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.text.WordUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scrollable widget that displays all goals with icons for selecting exclusions.
 */
@Environment(EnvType.CLIENT)
public class BoardTypeGoalListWidget extends AbstractScrollArea {

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

    public BoardTypeGoalListWidget(int x, int y, int width, int height, Component message, BoardTypeCreatorScreen parent) {
        super(x, y, width, height, message, AbstractScrollArea.defaultSettings(ENTRY_HEIGHT));
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
                    List<net.minecraft.world.item.Item> items = cycleProvider.getItemsToDisplay();
                    if (items != null && !items.isEmpty()) {
                        icon = items.get(0).getDefaultInstance();
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
            return SpawnEggItem.byId(entityType)
                .map(Holder::value)
                .map(item -> item.getDefaultInstance())
                .orElse(null);
        }

        return null;
    }
    
    private ItemStack getFallbackIconForGoal(String goalId) {
        String lowerGoalId = goalId.toLowerCase();
        
        if (lowerGoalId.contains("kill")) {
            return Items.WOODEN_SWORD.getDefaultInstance();
        }
        if (lowerGoalId.contains("die")) {
            return Items.SKELETON_SKULL.getDefaultInstance();
        }
        if (lowerGoalId.contains("breed")) {
            return Items.WHEAT.getDefaultInstance();
        }
        if (lowerGoalId.contains("tame")) {
            return Items.BONE.getDefaultInstance();
        }
        
        return Items.PAPER.getDefaultInstance();
    }
    
    @SuppressWarnings("deprecation")
    private String formatGoalName(String goalId) {
        String name = goalId.contains(":") ? goalId.substring(goalId.indexOf(":") + 1) : goalId;
        return WordUtils.capitalizeFully(name.replace("_", " "));
    }

    @Override
    protected int contentHeight() {
        return visibleGoals.size() * ENTRY_HEIGHT + 8;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.rowWidth = getWidth() - MARGIN_X * 2;
        this.left = getX() + MARGIN_X;
        this.top = getY();
        this.right = getX() + getWidth() - MARGIN_X;

        hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;

        context.enableScissor(this.left - 1, this.top, this.right + 1, getY() + getHeight());

        int y = 4;
        for (GoalEntry entry : visibleGoals) {
            int entryY = getY() + y - (int) scrollAmount();
            
            if (entryY + ENTRY_HEIGHT >= this.top && entryY < this.top + getHeight()) {
                entry.render(context, getX() + MARGIN_X, entryY, rowWidth, ENTRY_HEIGHT, 
                    mouseX, mouseY, Objects.equals(entry, hoveredEntry), delta);
            }
            
            y += ENTRY_HEIGHT;
        }

        context.disableScissor();
        this.extractScrollbar(context, mouseX, mouseY);
    }

    protected final GoalEntry getEntryAtPosition(double x, double y) {
        int relativeY = (int) (y - getY() + scrollAmount() - 4);
        int index = relativeY / ENTRY_HEIGHT;
        
        if (index >= 0 && index < visibleGoals.size()) {
            return visibleGoals.get(index);
        }
        return null;
    }

    public void updateSearch(String search) {
        setScrollAmount(0);
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        boolean scrollbarClicked = updateScrolling(click);
        if (hoveredEntry != null && click.button() == 0 && !scrollbarClicked) {
            parent.toggleGoalExclusion(hoveredEntry.goalId);
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
            return true;
        }
        return super.mouseClicked(click, doubleClick) || scrollbarClicked;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    public class GoalEntry {
        private final String goalId;
        private final String displayName;
        private final ItemStack icon;

        public GoalEntry(String goalId, String goalName, ItemStack icon) {
            this.goalId = goalId;
            this.displayName = goalName;
            this.icon = icon;
        }

        public void render(GuiGraphicsExtractor context, int x, int y, int width, int height, 
                          int mouseX, int mouseY, boolean hovered, float delta) {
            Font font = Minecraft.getInstance().font;
            boolean isExcluded = parent.isGoalExcluded(goalId);

            if (isExcluded) {
                context.fill(x, y, x + width, y + height, 0x80FF4040);
            } else if (hovered) {
                context.fill(x, y, x + width, y + height, 0x80FFFFFF);
            }

            int iconX = x + ICON_MARGIN;
            int iconY = y + (height - ICON_SIZE) / 2;
            if (icon != null) {
                context.item(icon, iconX, iconY);
            }

            int textX = iconX + ICON_SIZE + ICON_MARGIN * 2;
            int textY = y + (height - font.lineHeight) / 2;
            
            String drawText = displayName;
            int maxTextWidth = width - (ICON_SIZE + ICON_MARGIN * 3 + 5);
            if (font.width(drawText) > maxTextWidth) {
                while (font.width(drawText + "...") > maxTextWidth && drawText.length() > 0) {
                    drawText = drawText.substring(0, drawText.length() - 1);
                }
                drawText = drawText + "...";
            }

            context.text(font, drawText, textX, textY, 0xFFFFFFFF, false);

            if (hovered) {
                String idText = "[" + goalId + "]";
                int idWidth = font.width(idText);
                context.text(font, idText, x + width - idWidth - 5, textY, 
                    0xFF808080, false);
            }
        }
    }
}
