package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetAnySpyglassAdvancementGoal extends AdvancementGoal implements TextureProvider {

    private static final List<Identifier> ADVANCEMENTS = List.of(
            Identifier.of("minecraft", "adventure/spyglass_at_parrot"),
            Identifier.of("minecraft", "adventure/spyglass_at_ghast"),
            Identifier.of("minecraft", "adventure/spyglass_at_dragon")
            );

    public GetAnySpyglassAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Get any Spyglass Advancement";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/spyglass/spyglass_advancement.png");

    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
