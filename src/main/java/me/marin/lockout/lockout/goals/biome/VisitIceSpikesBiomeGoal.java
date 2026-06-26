package me.marin.lockout.lockout.goals.biome;

import me.marin.lockout.lockout.interfaces.VisitBiomeGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class VisitIceSpikesBiomeGoal extends VisitBiomeGoal {

    private static final ItemStack ITEM_STACK = Items.PACKED_ICE.getDefaultInstance();

    public VisitIceSpikesBiomeGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Find Ice Spikes Biome";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final List<Identifier> BIOME_LIST = List.of(Identifier.fromNamespaceAndPath("minecraft", "ice_spikes"));
    @Override
    public List<Identifier> getBiomes() {
        return BIOME_LIST;
    }

}
