package me.marin.lockout.lockout.goals.status_effect;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.StatusEffectGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class GetInfestedStatusEffectGoal extends StatusEffectGoal implements TextureProvider {

    public GetInfestedStatusEffectGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Get Infested";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public MobEffect getStatusEffect() {
        return MobEffects.INFESTED.value();
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/status_effect/infested.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
    
}
