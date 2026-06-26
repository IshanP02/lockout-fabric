package me.marin.lockout.lockout.goals.breed_animals;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.BreedAnimalGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.resources.Identifier;

public class BreedFoxGoal extends BreedAnimalGoal implements TextureProvider {

    public BreedFoxGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Breed Foxes";
    }

    @Override
    public EntityType<?> getAnimal() {
        return EntityTypes.FOX;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/breed/breed_fox.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
