package me.marin.lockout.lockout.goals.opponent;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.OpponentObtainsItemGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class OpponentObtainsSeedsGoal extends OpponentObtainsItemGoal implements TextureProvider {

    public OpponentObtainsSeedsGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getMessage(Player player) {
        return player.getName().getString() + " obtained Wheat Seeds.";
    }

    @Override
    public String getGoalName() {
        return "Opponent obtains Wheat Seeds";
    }

    private static final List<Item> ITEMS = List.of(Items.WHEAT_SEEDS);
    @Override
    public List<Item> getItems() {
        return ITEMS;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/opponent/no_seeds.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        return TextureProvider.super.renderTexture(context, x, y, tick);
    }
}
