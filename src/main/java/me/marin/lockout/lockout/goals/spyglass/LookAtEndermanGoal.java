package me.marin.lockout.lockout.goals.spyglass;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LookAtMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class LookAtEndermanGoal extends LookAtMobGoal implements TextureProvider {

    public LookAtEndermanGoal(String id, String data) {
        super(id, data, EntityTypes.ENDERMAN);
    }

    @Override
    public String getGoalName() {
        return "Look at Enderman with Spyglass";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/spyglass/spyglass_enderman.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
