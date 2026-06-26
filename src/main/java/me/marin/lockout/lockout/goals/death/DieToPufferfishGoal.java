package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DieToEntityGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.resources.Identifier;

public class DieToPufferfishGoal extends DieToEntityGoal implements TextureProvider {

    public DieToPufferfishGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die to Pufferfish";
    }

    @Override
    public EntityType getEntityType() {
        return EntityTypes.PUFFERFISH;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/death/die_to_pufferfish.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}