package me.marin.lockout.lockout.goals.name;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.NameMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class NameVindicatorGoal extends NameMobGoal implements TextureProvider {

    public NameVindicatorGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.VINDICATOR;
    }

    @Override
    public String getRequiredName() {
        return "Johnny";
    }

    @Override
    public String getGoalName() {
        return "Name a Vindicator \"Johnny\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/name/name_vindicator.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
