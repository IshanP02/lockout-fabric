package me.marin.lockout.lockout.goals.name;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.NameMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class NameIronGolemGoal extends NameMobGoal implements TextureProvider {

    public NameIronGolemGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.IRON_GOLEM;
    }

    @Override
    public String getRequiredName() {
        return "Dinnerbone";
    }

    @Override
    public String getGoalName() {
        return "Name a Iron Golem \"Dinnerbone\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/name/name_iron_golem.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
