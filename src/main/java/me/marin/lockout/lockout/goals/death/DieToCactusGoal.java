package me.marin.lockout.lockout.goals.death;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DieToDamageTypeGoal;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;

public class DieToCactusGoal extends DieToDamageTypeGoal {

    public DieToCactusGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Die to Cactus";
    }

    @Override
    public List<RegistryKey<DamageType>> getDamageRegistryKeys() {
        return List.of(DamageTypes.CACTUS);
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/death/die_to_cactus.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
