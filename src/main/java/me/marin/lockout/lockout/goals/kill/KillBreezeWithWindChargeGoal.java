package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class KillBreezeWithWindChargeGoal extends KillMobGoal implements TextureProvider {

    public KillBreezeWithWindChargeGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public EntityType<?> getEntity() {
        return EntityTypes.BREEZE;
    }

    @Override
    public String getGoalName() {
        return "Kill Breeze using Wind Charge";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/kill/breeze_wind_charge.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
