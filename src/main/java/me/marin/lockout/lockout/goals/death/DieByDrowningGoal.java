package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DieToDamageTypeGoal;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.List;

public class DieByDrowningGoal extends DieToDamageTypeGoal {

    public DieByDrowningGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die by Drowning";
    }

    @Override
    public List<ResourceKey<DamageType>> getDamageRegistryKeys() {
        return List.of(DamageTypes.DROWN);
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/death/die_to_drowning.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}