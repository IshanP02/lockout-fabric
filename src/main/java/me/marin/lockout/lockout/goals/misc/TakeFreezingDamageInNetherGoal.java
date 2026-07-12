package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class TakeFreezingDamageInNetherGoal extends Goal implements TextureProvider {

    public TakeFreezingDamageInNetherGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Take Freezing Damage in The Nether";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/freezing_damage_nether.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
