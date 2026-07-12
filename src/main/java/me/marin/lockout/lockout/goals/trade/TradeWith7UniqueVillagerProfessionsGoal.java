package me.marin.lockout.lockout.goals.trade;

import me.marin.lockout.Constants;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.Utility;
import me.marin.lockout.lockout.interfaces.TradeWithXUniqueProfessionsGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class TradeWith7UniqueVillagerProfessionsGoal extends TradeWithXUniqueProfessionsGoal implements TextureProvider {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/trade_with_x_professions.png");

    public TradeWith7UniqueVillagerProfessionsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Trade with 7 Unique Villager Professions";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public int getAmount() {
        return 7;
    }

    @Override
    public Map<LockoutTeam, LinkedHashSet<VillagerProfession>> getTrackerMap() {
        return LockoutServer.lockout.villagerProfessionsTraded;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        TextureProvider.super.renderTexture(context, x, y, tick);
        Utility.drawStackCount(context, x, y, "7");
        return true;
    }

    @Override
    public List<String> getTooltip(LockoutTeam team, Player player) {
        List<String> tooltip = new ArrayList<>();
        
        LinkedHashSet<VillagerProfession> professions = getTrackerMap().get(team);
        int count = professions != null ? professions.size() : 0;

        tooltip.add(" ");
        tooltip.add("Professions traded: " + count + "/" + getAmount());
        tooltip.add(" ");

        return tooltip;
    }

}
