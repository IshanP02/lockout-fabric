package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.IncrementStatGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.resources.Identifier;

import java.util.List;

public class PutFlowerInPotGoal extends IncrementStatGoal implements TextureProvider {

    public PutFlowerInPotGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> STATS = List.of(Stats.POT_FLOWER);
    @Override
    public List<Identifier> getStats() {
        return STATS;
    }

    @Override
    public String getGoalName() {
        return "Pot any plant in Flower Pot";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/flower_in_a_pot.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
