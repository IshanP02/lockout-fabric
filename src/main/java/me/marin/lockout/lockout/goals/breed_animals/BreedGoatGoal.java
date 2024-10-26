package me.marin.lockout.lockout.goals.breed_animals;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.BreedAnimalGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class BreedGoatGoal extends BreedAnimalGoal implements TextureProvider {

    public BreedGoatGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Breed Goats";
    }

    @Override
    public EntityType<?> getAnimal() {
        return EntityType.GOAT;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/breed/breed_goat.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
