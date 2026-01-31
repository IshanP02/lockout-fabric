package me.marin.lockout.lockout.goals.spyglass;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LookAtMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LookAtIronGolemGoal extends LookAtMobGoal implements TextureProvider {

    public LookAtIronGolemGoal(String id, String data) {
        super(id, data, EntityType.IRON_GOLEM);
    }

    @Override
    public String getGoalName() {
        return "Look at Iron Golem with Spyglass";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/spyglass/spyglass_iron_golem.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
