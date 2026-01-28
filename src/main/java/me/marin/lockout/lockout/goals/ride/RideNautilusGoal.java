package me.marin.lockout.lockout.goals.ride;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.RideEntityGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class RideNautilusGoal extends RideEntityGoal implements TextureProvider {

    public RideNautilusGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntityType() {
        return EntityType.NAUTILUS;
    }

    @Override
    public String getGoalName() {
        return "Ride a Nautilus";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/ride_nautilus.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}