package me.marin.lockout.lockout.goals.name;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.NameMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class NameSheepJebGoal extends NameMobGoal implements TextureProvider {

    public NameSheepJebGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.SHEEP;
    }

    @Override
    public String getRequiredName() {
        return "jeb_";
    }

    @Override
    public String getGoalName() {
        return "Name a Sheep \"jeb_\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/name/name_sheep.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
