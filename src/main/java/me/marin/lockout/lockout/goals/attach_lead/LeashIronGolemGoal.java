package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LeashIronGolemGoal extends LeashMobGoal implements TextureProvider {

    public LeashIronGolemGoal(String id, String data) {
        super(id, data, EntityType.IRON_GOLEM);
    }

    @Override
    public String getGoalName() {
        return "Attach Lead to Iron Golem";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/attach_lead/leash_iron_golem.png");
    
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
