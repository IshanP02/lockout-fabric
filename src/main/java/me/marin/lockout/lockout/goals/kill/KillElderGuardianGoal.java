package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class KillElderGuardianGoal extends KillMobGoal implements TextureProvider {

    public KillElderGuardianGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill Elder Guardian";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/kill/kill_elder_guardian.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public EntityType<?> getEntity() {
        return EntityTypes.ELDER_GUARDIAN;
    }
}
