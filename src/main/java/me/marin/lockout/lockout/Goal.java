package me.marin.lockout.lockout;

import lombok.Getter;
import lombok.Setter;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.client.LockoutClient;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Objects;

public abstract class Goal {

    @Getter
    private final String id;
    @Getter
    private final String data;
    private boolean isCompleted = false;
    @Getter
    private LockoutTeam completedTeam;
    @Getter @Setter
    private String CompletedMessage;

    public Goal(String id, String data) {
        this.id = id;
        this.data = data;
        this.CompletedMessage = "";
    }

    public abstract String getGoalName();

    /**
     * Displays this ItemStack on the board.
     * Also used as a fallback if CustomTextureRenderer fails to render (returns false).
     */
    public abstract ItemStack getTextureItemStack();

    public void setCompleted(boolean isCompleted, LockoutTeam team) {
        this.isCompleted = isCompleted;
        this.completedTeam = team;
    }

    public void setCompleted(boolean isCompleted, LockoutTeam team, String playerName) {
        setCompleted(isCompleted, team);
        setCompletedMessage(playerName);
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public final void render(DrawContext context, TextRenderer textRenderer, int x, int y) {
        boolean success = false;
        if (this instanceof CustomTextureRenderer customTextureRenderer) {
            success = customTextureRenderer.renderTexture(context, x, y, LockoutClient.CURRENT_TICK);
        }
        if (!success) {
            // If the icon is a player head, render a flat 2D face (with hat) instead
            ItemStack textureStack = this.getTextureItemStack();
            if (textureStack != null && textureStack.getItem() == net.minecraft.item.Items.PLAYER_HEAD) {
                Identifier defaultSkin = Identifier.of("minecraft", "textures/entity/steve.png");
                // Draw scaled 16x16 face (face at 8,8 in the skin) and the hat overlay (at 40,8)
                context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, defaultSkin, x, y, 8, 8, 16, 16, 64, 64);
                context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, defaultSkin, x, y, 40, 8, 16, 16, 64, 64);
            } else {
                // Fallback: draw the ItemStack normally
                context.drawItem(textureStack, x, y);
                context.drawStackOverlay(textRenderer, textureStack, x, y);
            }
        }
    }

    public boolean hasData() {
        return data != null && !Objects.equals(data, GoalDataConstants.DATA_NONE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Goal goal = (Goal) o;
        return id.equals(goal.id) && Objects.equals(data, goal.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data);
    }

}
