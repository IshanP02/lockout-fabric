package me.marin.lockout.lockout.goals.kill;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.goals.util.GoalDataConstants;
import me.marin.lockout.lockout.interfaces.KillMobGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.resources.Identifier;

public class KillColoredSheepGoal extends KillMobGoal implements TextureProvider {

    private static final Item ITEM = Items.SHEEP_SPAWN_EGG;

    private final Identifier texture;
    private final String GOAL_NAME;
    private final DyeColor DYE_COLOR;

    public KillColoredSheepGoal(String id, String data) {
        super(id, data);
        texture = Identifier.fromNamespaceAndPath(Constants.NAMESPACE, "textures/custom/sheep/kill_" + data + "_sheep.png");
        DYE_COLOR = GoalDataConstants.getDyeColor(data);
        GOAL_NAME = "Kill " + GoalDataConstants.getDyeColorFormatted(DYE_COLOR) + " Sheep";
    }

    @Override
    public EntityType<?> getEntity() {
        return EntityTypes.SHEEP;
    }

    public DyeColor getDyeColor() {
        return DYE_COLOR;
    }

    @Override
    public String getGoalName() {
        return GOAL_NAME;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

    @Override
    public Identifier getTextureIdentifier() {
        return texture;
    }

}
