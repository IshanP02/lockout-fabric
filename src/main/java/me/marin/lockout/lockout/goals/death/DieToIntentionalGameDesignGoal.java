package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DieToDamageTypeGoal;
import me.marin.lockout.lockout.texture.CycleTexturesProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.List;

public class DieToIntentionalGameDesignGoal extends DieToDamageTypeGoal implements CycleTexturesProvider {

    public DieToIntentionalGameDesignGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die to [Intentional Game Design]";
    }

    @Override
    public List<ResourceKey<DamageType>> getDamageRegistryKeys() {
        return List.of(DamageTypes.BAD_RESPAWN_POINT);
    }

    @Override
    public Identifier getTextureIdentifier() {
        return null;
    }

    @Override
    public boolean renderTexture(GuiGraphicsExtractor context, int x, int y, int tick) {
        return CycleTexturesProvider.super.renderTexture(context, x, y, tick);
    }
    private static final List<Identifier> TEXTURES = List.of(
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/death/die_to_anchor.png"),
            Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/death/die_to_bed.png")
    );

    @Override
    public List<Identifier> getTexturesToDisplay() {
        return TEXTURES;
    }

}
