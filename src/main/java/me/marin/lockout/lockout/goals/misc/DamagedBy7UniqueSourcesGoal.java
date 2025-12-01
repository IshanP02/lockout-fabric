package me.marin.lockout.lockout.goals.misc;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.DamagedByUniqueSourcesGoal;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class DamagedBy7UniqueSourcesGoal extends DamagedByUniqueSourcesGoal {

    private static final ItemStack ITEM_STACK = Items.TERRACOTTA.getDefaultStack();

    public DamagedBy7UniqueSourcesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Take Damage from 7 Unique Sources";
    }

    @Override
    public int getAmount() {
        return 7;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/take_unique_damage.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}