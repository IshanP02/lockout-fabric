package me.marin.lockout.lockout.goals.dimension;

import me.marin.lockout.lockout.interfaces.EnterDimensionGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import static me.marin.lockout.Constants.NAMESPACE;

public class EnterNetherGoal extends EnterDimensionGoal implements TextureProvider {

    public EnterNetherGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Enter Nether";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    @Override
    public ResourceKey<Level> getWorldRegistryKey() {
        return Level.NETHER;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(NAMESPACE, "textures/custom/nether_portal.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}
