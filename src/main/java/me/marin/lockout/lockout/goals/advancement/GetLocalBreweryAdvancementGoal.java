package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GetLocalBreweryAdvancementGoal extends AdvancementGoal {

    private static final ItemStack ITEM_STACK = Items.BREWING_STAND.getDefaultInstance();

    public GetLocalBreweryAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Use Brewing Stand";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM_STACK;
    }

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.fromNamespaceAndPath("minecraft", "nether/brew_potion"));
    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
