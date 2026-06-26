package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GetATerribleFortressAdvancementGoal extends AdvancementGoal {

    private static final ItemStack ITEM_STACK = Items.NETHER_BRICKS.getDefaultInstance();
    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.fromNamespaceAndPath("minecraft", "nether/find_fortress"));

    public GetATerribleFortressAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Find a Fortress";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
