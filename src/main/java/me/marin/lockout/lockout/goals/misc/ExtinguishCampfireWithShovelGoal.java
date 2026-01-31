package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ExtinguishCampfireWithShovelGoal extends Goal implements TextureProvider {

    public ExtinguishCampfireWithShovelGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Extinguish Campfire with Shovel";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/unlit_campfire.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
