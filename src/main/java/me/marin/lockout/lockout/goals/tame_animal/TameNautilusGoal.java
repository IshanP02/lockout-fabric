package me.marin.lockout.lockout.goals.tame_animal;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.TameAnimalGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class TameNautilusGoal extends TameAnimalGoal implements TextureProvider {

    public TameNautilusGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getAnimal() {
        return EntityTypes.NAUTILUS;
    }

    @Override
    public boolean matchesAnimal(EntityType<?> entityType) {
        return entityType == EntityTypes.NAUTILUS || entityType == EntityTypes.ZOMBIE_NAUTILUS;
    }

    @Override
    public String getGoalName() {
        return "Tame a Nautilus";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/tame/tame_nautilus.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}