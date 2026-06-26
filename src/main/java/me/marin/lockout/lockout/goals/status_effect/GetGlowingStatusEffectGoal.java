package me.marin.lockout.lockout.goals.status_effect;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.StatusEffectGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class GetGlowingStatusEffectGoal extends StatusEffectGoal implements TextureProvider {

    public GetGlowingStatusEffectGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Get Glowing";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public MobEffect getStatusEffect() {
        return MobEffects.GLOWING.value();
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/status_effect/glowing.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
    
}
