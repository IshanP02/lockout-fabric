package me.marin.lockout.lockout.goals.spyglass;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LookAtMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LookAtHuskGoal extends LookAtMobGoal implements TextureProvider {

    public LookAtHuskGoal(String id, String data) {
        super(id, data, EntityType.HUSK);
    }

    @Override
    public String getGoalName() {
        return "Look at Husk with Spyglass";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/spyglass/spyglass_husk.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
