package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LeashCherryChestBoatGoal extends LeashMobGoal implements TextureProvider {

    public LeashCherryChestBoatGoal(String id, String data) {
        super(id, data, EntityType.CHERRY_CHEST_BOAT);
    }

    @Override
    public String getGoalName() {
        return "Attach Lead to Cherry Chest Boat";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/attach_lead/leash_chest_boat.png");
    
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
