package me.marin.lockout.lockout.goals.biome;

import me.marin.lockout.lockout.interfaces.VisitBiomeGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class VisitBadlandsBiomeGoal extends VisitBiomeGoal {

    private static final ItemStack ITEM_STACK = Items.TERRACOTTA.getDefaultInstance();

    public VisitBadlandsBiomeGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Find Badlands Biome";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final List<Identifier> BIOME_LIST = List.of(
            Identifier.fromNamespaceAndPath("minecraft", "badlands"),
            Identifier.fromNamespaceAndPath("minecraft", "eroded_badlands"),
            Identifier.fromNamespaceAndPath("minecraft", "wooded_badlands")
    );
    @Override
    public List<Identifier> getBiomes() {
        return BIOME_LIST;
    }

}
