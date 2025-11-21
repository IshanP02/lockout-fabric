package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetUnderLockAndKeyAdvancementGoal extends AdvancementGoal {

    private static final Item ITEM = Items.TRIAL_KEY;

    public GetUnderLockAndKeyAdvancementGoal(String id, String data) {
        super(id, data);
    }

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "adventure/under_lock_and_key"));
    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }

    @Override
    public String getGoalName() {
        return "Unlock a Vault with Trial Key";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return ITEM.getDefaultStack();
    }

}