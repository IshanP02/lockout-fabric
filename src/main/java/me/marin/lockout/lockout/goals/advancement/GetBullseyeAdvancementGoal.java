package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GetBullseyeAdvancementGoal extends AdvancementGoal {

    private static final Item ITEM = Items.TARGET;

    public GetBullseyeAdvancementGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.fromNamespaceAndPath("minecraft", "adventure/bullseye"));
    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }

    @Override
    public String getGoalName() {
        return "Obtain \"Bullseye\" Advancement";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultInstance();
    }

}
