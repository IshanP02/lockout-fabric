package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class ShootFireworkFromCrossbowGoal extends Goal implements TextureProvider {

    public ShootFireworkFromCrossbowGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Shoot Firework from Crossbow";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/item/crossbow_firework.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
