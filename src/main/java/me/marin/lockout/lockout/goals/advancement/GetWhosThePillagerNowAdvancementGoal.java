package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetWhosThePillagerNowAdvancementGoal extends AdvancementGoal implements TextureProvider{

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "adventure/whos_the_pillager_now"));

    public GetWhosThePillagerNowAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Kill Pillager with a Crossbow";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/kill/kill_pillager_with_crossbow.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
