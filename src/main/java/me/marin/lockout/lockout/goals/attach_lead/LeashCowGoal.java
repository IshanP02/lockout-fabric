package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class LeashCowGoal extends LeashMobGoal implements TextureProvider {

    public LeashCowGoal(String id, String data) {
        super(id, data, EntityTypes.COW);
    }

    @Override
    public String getGoalName() {
        return "Attach Lead to Cow";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/attach_lead/leash_cow.png");
    
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
