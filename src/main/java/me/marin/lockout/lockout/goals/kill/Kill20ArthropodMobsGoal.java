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

public class Kill20ArthropodMobsGoal extends KillSpecificMobsGoal implements CycleTexturesProvider {

    private static final ItemStack ITEM_STACK = Items.WOODEN_SWORD.getDefaultInstance();
    static {
        ITEM_STACK.setCount(20);
    }
    private static final List<Identifier> TEXTURES = List.of(
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/arthropod/kill_spider.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/arthropod/kill_bee.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/arthropod/kill_cave_spider.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/arthropod/kill_endermite.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/arthropod/kill_silverfish.png")
    );
    // arthropods: bee, cave spider, spider, endermite, silverfish
    private static final List<EntityType<?>> ARTHROPOD_MOBS = List.of(
            EntityTypes.BEE,
            EntityTypes.CAVE_SPIDER,
            EntityTypes.SPIDER,
            EntityTypes.ENDERMITE,
            EntityTypes.SILVERFISH
    );

    public Kill20ArthropodMobsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill 20 Arthropods";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<EntityType<?>> getEntityTypes() {
        return ARTHROPOD_MOBS;
    }

    @Override
    public int getAmount() {
        return 20;
    }

    @Override
    public Map<LockoutTeam, Integer> getTrackerMap() {
        return LockoutServer.lockout.killedArthropods;
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
        tooltip.add("Arthropods killed: " + getTrackerMap().getOrDefault(team, 0) + "/" + getAmount());
        tooltip.add(" ");

        return tooltip;
    }

}
