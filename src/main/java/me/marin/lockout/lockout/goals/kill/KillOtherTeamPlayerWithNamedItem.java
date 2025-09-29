package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class KillOtherTeamPlayerWithNamedItem extends Goal implements TextureProvider {

    public KillOtherTeamPlayerWithNamedItem(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill another team's player with a named item";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/kill_player.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
