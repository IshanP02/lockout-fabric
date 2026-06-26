package me.marin.lockout.lockout.interfaces;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public abstract class DieToDamageTypeGoal extends Goal implements TextureProvider {

    public DieToDamageTypeGoal(String id, String data) {
        super(id, data);
    }

    public abstract List<ResourceKey<DamageType>> getDamageRegistryKeys();

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

}
