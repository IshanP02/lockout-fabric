package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DieToDamageTypeGoal;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;

public class DieToTridentGoal extends DieToDamageTypeGoal {

    public DieToTridentGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die to Trident";
    }

    @Override
    public List<RegistryKey<DamageType>> getDamageRegistryKeys() {
        return List.of(DamageTypes.TRIDENT);
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/death/die_to_trident.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
