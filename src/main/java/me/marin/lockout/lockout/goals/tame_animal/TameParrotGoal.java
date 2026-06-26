package me.marin.lockout.lockout.goals.tame_animal;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.TameAnimalGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class TameParrotGoal extends TameAnimalGoal implements TextureProvider {

    public TameParrotGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Tame a Parrot";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/tame/tame_parrot.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public EntityType<?> getAnimal() {
        return EntityTypes.PARROT;
    }
}
