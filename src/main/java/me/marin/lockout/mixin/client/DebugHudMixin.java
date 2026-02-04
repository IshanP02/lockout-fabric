package me.marin.lockout.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the debug HUD's right column into the left column to prevent overlap with the lockout board.
 * This approach is robust against user debug settings and future Minecraft updates.
 */
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    /**
     * Invoker to access the private drawText method.
     */
    @Invoker("drawText")
    protected abstract void lockout$invokeDrawText(
            DrawContext context,
            List<String> text,
            boolean left
    );

    @Unique
    private List<String> lockout$rightText = new ArrayList<>();

    /**
     * Filter out lines containing "#" (typically tag/property information)
     * and remove consecutive empty lines.
     */
    @Unique
    private List<String> lockout$filterTargetedInfo(List<String> text) {
        List<String> filtered = new ArrayList<>();
        boolean lastWasEmpty = false;
        
        for (String line : text) {
            // Skip lines containing "#"
            if (line.contains("#")) {
                continue;
            }
            
            // Check if this line is empty
            boolean isEmpty = line.trim().isEmpty();
            
            // Skip if this is empty and the last line was also empty
            if (isEmpty && lastWasEmpty) {
                continue;
            }
            
            filtered.add(line);
            lastWasEmpty = isEmpty;
        }
        
        return filtered;
    }

    /**
     * Reorder debug lines according to a specific layout.
     */
    @Unique
    private List<String> lockout$reorderDebugLines(List<String> text) {
        // Storage for categorized lines
        String mcVersion = null;
        String fpsCounter = null;
        String cCounter = null;
        String eCounter = null;
        String biome = null;
        String playerCoords = null;
        String block = null;
        String chunk = null;
        String facing = null;
        String dimension = null;
        String sectionRelative = null;
        String heightmap1 = null;
        String heightmap2 = null;
        List<String> targetedBlock = new ArrayList<>();
        List<String> targetedFluid = new ArrayList<>();
        List<String> targetedEntity = new ArrayList<>();
        String debugChartHelp = null;
        String editDebugChartHelp = null;
        List<String> uncategorized = new ArrayList<>();
        
        // Track which targeted section we're in
        String currentTargetedSection = null;
        
        for (String line : text) {
            if (line.trim().isEmpty()) {
                continue; // We'll add empty lines manually in the correct places
            }
            
            // Check for targeted sections (these are multi-line)
            if (line.contains("Targeted Block")) {
                currentTargetedSection = "block";
                targetedBlock.add(line);
                continue;
            } else if (line.contains("Targeted Fluid")) {
                currentTargetedSection = "fluid";
                targetedFluid.add(line);
                continue;
            } else if (line.contains("Targeted Entity")) {
                currentTargetedSection = "entity";
                targetedEntity.add(line);
                continue;
            }
            
            // If we're in a targeted section, keep adding lines until we hit another category
            if (currentTargetedSection != null) {
                // Check if this line starts a new non-targeted category
                if (line.contains("1.21") || line.contains("vsync") || line.contains("pU:") || 
                    line.contains("SD:") || line.contains("Biome") || line.contains("XYZ:") ||
                    line.contains("Block:") || line.contains("Chunk:") || line.contains("Facing:") ||
                    line.contains("FC:") || line.contains("Section-relative") || line.contains("CH S:") ||
                    line.contains("SH S:") || line.contains("Debug charts:") || line.contains("To edit:")) {
                    currentTargetedSection = null;
                    // Fall through to categorize this line
                } else {
                    // Add to current targeted section
                    if (currentTargetedSection.equals("block")) {
                        targetedBlock.add(line);
                    } else if (currentTargetedSection.equals("fluid")) {
                        targetedFluid.add(line);
                    } else if (currentTargetedSection.equals("entity")) {
                        targetedEntity.add(line);
                    }
                    continue;
                }
            }
            
            // Categorize individual lines
            if (line.contains("1.21")) {
                mcVersion = line;
            } else if (line.contains("vsync")) {
                fpsCounter = line;
            } else if (line.contains("pU:")) {
                cCounter = line;
            } else if (line.contains("SD:")) {
                eCounter = line;
            } else if (line.contains("Biome")) {
                biome = line;
            } else if (line.contains("XYZ:")) {
                playerCoords = line;
            } else if (line.contains("Block:")) {
                block = line;
            } else if (line.contains("Chunk:")) {
                chunk = line;
            } else if (line.contains("Facing:")) {
                facing = line;
            } else if (line.contains("FC:")) {
                dimension = line;
            } else if (line.contains("Section-relative")) {
                sectionRelative = line;
            } else if (line.contains("CH S:")) {
                heightmap1 = line;
            } else if (line.contains("SH S:")) {
                heightmap2 = line;
            } else if (line.contains("Debug charts:")) {
                debugChartHelp = line;
            } else if (line.contains("To edit:")) {
                editDebugChartHelp = line;
            } else {
                // This is an uncategorized line
                uncategorized.add(line);
            }
        }
        
        // Rebuild in the specified order
        List<String> reordered = new ArrayList<>();
        
        if (mcVersion != null) reordered.add(mcVersion);
        if (fpsCounter != null) reordered.add(fpsCounter);
        reordered.add("");
        if (cCounter != null) reordered.add(cCounter);
        if (eCounter != null) reordered.add(eCounter);
        reordered.add("");
        if (biome != null) reordered.add(biome);
        if (playerCoords != null) reordered.add(playerCoords);
        if (block != null) reordered.add(block);
        if (chunk != null) reordered.add(chunk);
        if (facing != null) reordered.add(facing);
        if (dimension != null) reordered.add(dimension);
        if (sectionRelative != null) reordered.add(sectionRelative);
        reordered.add("");
        if (heightmap1 != null) reordered.add(heightmap1);
        if (heightmap2 != null) reordered.add(heightmap2);
        reordered.add("");
        if (!targetedBlock.isEmpty()) {
            reordered.addAll(targetedBlock);
            reordered.add("");
        }
        if (!targetedFluid.isEmpty()) {
            reordered.addAll(targetedFluid);
            reordered.add("");
        }
        if (!targetedEntity.isEmpty()) {
            reordered.addAll(targetedEntity);
            reordered.add("");
        }
        // Add uncategorized lines before debug chart help
        if (!uncategorized.isEmpty()) {
            reordered.add("");
            reordered.addAll(uncategorized);
        }
        if (debugChartHelp != null) reordered.add(debugChartHelp);
        if (editDebugChartHelp != null) reordered.add(editDebugChartHelp);
        
        // Remove empty lines that aren't between two content lines
        return lockout$removeInvalidEmptyLines(reordered);
    }
    
    /**
     * Remove empty lines that aren't between two lines with text,
     * and prevent consecutive empty lines.
     */
    @Unique
    private List<String> lockout$removeInvalidEmptyLines(List<String> text) {
        List<String> filtered = new ArrayList<>();
        boolean lastWasEmpty = false;
        
        for (int i = 0; i < text.size(); i++) {
            String line = text.get(i);
            
            if (line.trim().isEmpty()) {
                // Skip if the last line we added was also empty
                if (lastWasEmpty) {
                    continue;
                }
                
                // Check if this empty line is between two non-empty lines
                boolean hasPrevious = false;
                boolean hasNext = false;
                
                // Look backwards for previous non-empty line
                for (int j = i - 1; j >= 0; j--) {
                    if (!text.get(j).trim().isEmpty()) {
                        hasPrevious = true;
                        break;
                    }
                }
                
                // Look forwards for next non-empty line
                for (int j = i + 1; j < text.size(); j++) {
                    if (!text.get(j).trim().isEmpty()) {
                        hasNext = true;
                        break;
                    }
                }
                
                // Only keep empty line if it's between content
                if (hasPrevious && hasNext) {
                    filtered.add(line);
                    lastWasEmpty = true;
                }
            } else {
                filtered.add(line);
                lastWasEmpty = false;
            }
        }
        
        return filtered;
    }

    /**
     * Intercept all drawText calls from render() to merge right column into left.
     */
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/DebugHud;drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V"
        )
    )
    private void lockout$mergeDebugColumns(
            DebugHud instance,
            DrawContext context,
            List<String> text,
            boolean left
    ) {
        if (left) {
            // Filter targeted info from left column
            text = lockout$filterTargetedInfo(text);
            
            // Render left column with right content above it
            if (!lockout$rightText.isEmpty()) {
                List<String> merged = new ArrayList<>(lockout$rightText);
                merged.add("");
                merged.addAll(text);
                // Apply filter again to remove any double empty lines from the merge
                merged = lockout$filterTargetedInfo(merged);
                // Reorder all debug lines
                merged = lockout$reorderDebugLines(merged);
                lockout$invokeDrawText(context, merged, true);
                lockout$rightText.clear();
            } else {
                // Reorder debug lines for left-only display
                text = lockout$reorderDebugLines(text);
                lockout$invokeDrawText(context, text, true);
            }
        } else {
            // Capture and filter right column, don't render it
            lockout$rightText = lockout$filterTargetedInfo(new ArrayList<>(text));
        }
    }

}
