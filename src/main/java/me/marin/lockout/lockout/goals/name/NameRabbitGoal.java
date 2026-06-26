package me.marin.lockout.lockout.goals.name;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.NameMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class NameRabbitGoal extends NameMobGoal implements TextureProvider {

    public NameRabbitGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityTypes.RABBIT;
    }

    @Override
    public String getRequiredName() {
        return "Toast";
    }

    @Override
    public String getGoalName() {
        return "Name a Rabbit \"Toast\"";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/name/name_rabbit.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
