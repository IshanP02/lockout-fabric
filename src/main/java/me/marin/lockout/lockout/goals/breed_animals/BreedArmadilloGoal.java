package me.marin.lockout.lockout.goals.breed_animals;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.BreedAnimalGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.resources.Identifier;

public class BreedArmadilloGoal extends BreedAnimalGoal implements TextureProvider {

    public BreedArmadilloGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Breed Armadillos";
    }

    @Override
    public EntityType<?> getAnimal() {
        return EntityTypes.ARMADILLO;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/breed/breed_armadillo.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
