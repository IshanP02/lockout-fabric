package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.interfaces.KillAllSpecificMobsGoal;
import me.marin.lockout.lockout.texture.CustomTextureRenderer;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class KillAllRaidMobsGoal extends KillAllSpecificMobsGoal implements CustomTextureRenderer {

    private static final ItemStack DISPLAY_ITEM_STACK = Items.VILLAGER_SPAWN_EGG.getDefaultInstance();
    private static final List<EntityType<?>> MOBS = List.of(EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.RAVAGER, EntityTypes.WITCH, EntityTypes.VEX, EntityTypes.EVOKER);
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/status_effect/bad_omen.png");

    static {
        DISPLAY_ITEM_STACK.setCount(MOBS.size());
    }

    public KillAllRaidMobsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill all Raid Mobs";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return DISPLAY_ITEM_STACK;
    }

    @Override
    public List<EntityType<?>> getEntityTypes() {
        return MOBS;
    }

    @Override
    public Map<LockoutTeam, LinkedHashSet<EntityType<?>>> getTrackerMap() {
        return LockoutServer.lockout.killedRaidMobs;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        var raidMobs = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());

        tooltip.add(" ");
        tooltip.add("Raid mobs killed: " + raidMobs.size() + "/" + MOBS.size());
        tooltip.addAll(HasTooltipInfo.commaSeparatedList(raidMobs.stream().map(type -> type.getDescriptionId()).toList()));
        tooltip.add(" ");

        return tooltip;
    }

    @Override
    public List<String> getSpectatorTooltip() {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        for (LockoutTeam team : LockoutServer.lockout.getTeams()) {
            var raidMobs = getTrackerMap().getOrDefault(team, new LinkedHashSet<>());
            tooltip.add(team.getColor() + team.getDisplayName() + ChatFormatting.RESET + ": " + raidMobs.size() + "/" + MOBS.size());
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

}
