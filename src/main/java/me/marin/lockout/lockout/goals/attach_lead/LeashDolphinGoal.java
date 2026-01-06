package me.marin.lockout.lockout.goals.attach_lead;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.LeashMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LeashDolphinGoal extends LeashMobGoal implements TextureProvider {

    public LeashDolphinGoal(String id, String data) {
        super(id, data, EntityType.DOLPHIN);
    }

    @Override
    public String getGoalName() {
        return "Attach Lead to Dolphin";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/attach_lead/leash_dolphin.png");
    
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }
}
