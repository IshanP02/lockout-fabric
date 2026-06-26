package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GetHiredHelpAdvancementGoal extends AdvancementGoal {

    private static final Item ITEM = Items.IRON_BLOCK;

    public GetHiredHelpAdvancementGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.fromNamespaceAndPath("minecraft", "adventure/summon_iron_golem"));
    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }

    @Override
    public String getGoalName() {
        return "Construct an Iron Golem";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

}
