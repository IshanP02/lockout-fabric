package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class BreedUniqueAnimalsGoal extends Goal implements RequiresAmount, CustomTextureRenderer, HasTooltipInfo {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/breed/breed_x.png");
    private final ItemStack DISPLAY_ITEM_STACK = Items.WHEAT.getDefaultInstance();

    public BreedUniqueAnimalsGoal(String id, String data) {
        super(id, data);
        DISPLAY_ITEM_STACK.setCount(getAmount());
    }

    @Override
    public String getGoalName() {
        return String.format("Breed %d Unique Animals", getAmount());
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        var animals = LockoutServer.lockout.bredAnimalTypes.getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Animals bred: " + animals.size() + "/" + getAmount());
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(animals.stream().map(type -> type.getDescriptionId()).toList()));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            var animals = LockoutServer.lockout.bredAnimalTypes.getOrDefault(team, new LinkedHashSet<>());
            tooltip.add(team.getColor() + team.getDisplayName() + ChatFormatting.RESET + ": " + animals.size() + "/" + getAmount());
        }
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        context.itemDecorations(Minecraft.getInstance().font, DISPLAY_ITEM_STACK, x, y);
        return true;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

}
