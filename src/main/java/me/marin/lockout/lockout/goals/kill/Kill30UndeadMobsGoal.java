package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.lockout.interfaces.KillSpecificMobsGoal;
import me.marin.lockout.lockout.texture.CycleTexturesProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Kill30UndeadMobsGoal extends KillSpecificMobsGoal implements CycleTexturesProvider {

    private static final ItemStack ITEM_STACK = Items.WOODEN_SWORD.getDefaultInstance();
    static {
        ITEM_STACK.setCount(30);
    }
    private static final List<Identifier> TEXTURES = List.of(
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_zombie.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_wither_skeleton.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_zombie_villager.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_drowned.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_husk.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_stray.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/undead/kill_zoglin.png")
    );
    // undead: drowned, husk, phantom, skeleton, skeletonhorse, stray, wither, wither skeleton, zoglin, zombie, zombie horse, zombie villager, zombiefied piglin, bogged
    private static final List<EntityType<?>> UNDEAD_MOBS = List.of(
            EntityTypes.DROWNED,
            EntityTypes.HUSK,
            EntityTypes.PHANTOM,
            EntityTypes.SKELETON,
            EntityTypes.SKELETON_HORSE,
            EntityTypes.STRAY,
            EntityTypes.WITHER,
            EntityTypes.WITHER_SKELETON,
            EntityTypes.ZOGLIN,
            EntityTypes.ZOMBIE,
            EntityTypes.ZOMBIE_HORSE,
            EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.ZOMBIFIED_PIGLIN,
            EntityTypes.BOGGED,
            EntityTypes.PARCHED,
            EntityTypes.ZOMBIE_NAUTILUS,
            EntityTypes.CAMEL_HUSK
    );
    public Kill30UndeadMobsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill 30 Undead Mobs";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<EntityType<?>> getEntityTypes() {
        return UNDEAD_MOBS;
    }

    @Override
    public int getAmount() {
        return 30;
    }

    @Override
    public Map<LockoutTeam, Integer> getTrackerMap() {
        return LockoutServer.lockout.killedUndeadMobs;
    }

    @Override
    public List<Identifier> getTexturesToDisplay() {
        return TEXTURES;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        CycleTexturesProvider.super.renderTexture(context, x, y, tick);
        context.itemDecorations(Minecraft.getInstance().font, ITEM_STACK, x, y);
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();

        tooltip.add(" ");
        tooltip.add("Undead Mobs killed: " + getTrackerMap().getOrDefault(team, 0) + "/" + getAmount());
        tooltip.add(" ");

        return tooltip;
    }

}
