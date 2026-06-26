package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class KillZombieVillagerGoal extends KillMobGoal implements TextureProvider {

    public KillZombieVillagerGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill Zombie Villager";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/kill/kill_zombie_villager.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public EntityType<?> getEntity() {
        return EntityTypes.ZOMBIE_VILLAGER;
    }

}
