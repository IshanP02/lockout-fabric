package me.marin.lockout.lockout.goals.biome.unique;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.VisitUniqueBiomesGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class Visit10UniqueBiomesGoal extends VisitUniqueBiomesGoal implements TextureProvider {

    private static final ItemStack ITEM_STACK = Items.TERRACOTTA.getDefaultStack();

    public Visit10UniqueBiomesGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Visit 10 Unique Biomes";
    }

    @Override
    public int getAmount() {
        return 10;
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/unique_biomes.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

}