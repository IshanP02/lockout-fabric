package me.marin.lockout.lockout.goals.advancement;

import me.marin.lockout.Constants;
import me.marin.lockout.lockout.interfaces.AdvancementGoal;
import me.marin.lockout.lockout.texture.TextureProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

public class GetRemoteGatewayAdvancementGoal extends AdvancementGoal implements TextureProvider {

    public GetRemoteGatewayAdvancementGoal(String id, String data) {
        super(id, data);
    }

    @Override
    public String getGoalName() {
        return "Enter End Gateway";
    }

    @Override
    public ItemStack getTextureItemStack() {
        return null;
    }

    private static final Identifier TEXTURE = Identifier.of(Constants.NAMESPACE, "textures/custom/end_gateway.png");
    @Override
    public Identifier getTextureIdentifier() {
        return TEXTURE;
    }

    private static final List<Identifier> ADVANCEMENTS = List.of(Identifier.of("minecraft", "end/enter_end_gateway"));
    @Override
    public List<Identifier> getAdvancements() {
        return ADVANCEMENTS;
    }
}
