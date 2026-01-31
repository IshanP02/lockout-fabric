package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetStarTraderAdvancementGoal extends AdvancementGoal implements TextureProvider{

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "adventure/trade_at_world_height"));

    public GetStarTraderAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Obtain \"Star Trader\" advancement";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/star_trader.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
