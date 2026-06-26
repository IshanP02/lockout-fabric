package me.marin.lockout.lockout.goals.status_effect;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.StatusEffectGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class GetWeaknessStatusEffectGoal extends StatusEffectGoal implements TextureProvider {

    public GetWeaknessStatusEffectGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Get Weakness";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public MobEffect getStatusEffect() {
        return MobEffects.WEAKNESS.value();
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/status_effect/weakness.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
    
}
