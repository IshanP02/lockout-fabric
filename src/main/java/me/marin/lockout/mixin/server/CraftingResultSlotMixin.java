package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.have_more.HaveMostUniqueCraftsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mixin(ResultSlot.class)
public class CraftingResultSlotMixin {

    @Shadow @Final
    private Player player;

    @Inject(method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    public void onCraft(ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player.getUUID())) return;

        if (stack.isEmpty()) {
            return;
        }

        if (!(player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu)) return;

        lockout.uniqueCrafts.putIfAbsent(player.getUUID(), new HashSet<>());
        Set<Item> crafts = lockout.uniqueCrafts.get(player.getUUID());
        boolean addedNew = crafts.add(stack.getItem());

        if (!addedNew) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;

            if (goal instanceof HaveMostUniqueCraftsGoal) {
                // Play sound only to this specific player (client-side)
                if (player instanceof ServerPlayer serverPlayer) {
                    Holder<net.minecraft.sounds.SoundEvent> soundEntry = SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE;
                    serverPlayer.connection.send(
                        new ClientboundSoundPacket(
                            soundEntry,
                            SoundSource.MASTER,
                            serverPlayer.getX(),
                            serverPlayer.getY(),
                            serverPlayer.getZ(),
                            2f,
                            2f,
                            player.level().getRandom().nextLong()
                        )
                    );
                }
                if (crafts.size() % 5 == 0) {
                    player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "" + ChatFormatting.ITALIC + "You have crafted " + crafts.size() + " unique items."));
                }
                player.sendSystemMessage(Component.literal("Unique crafts: " + crafts.size()));

                if (crafts.size() > lockout.mostUniqueCrafts) {
                    if (!Objects.equals(lockout.mostUniqueCraftsPlayer, player.getUUID())) {
                        lockout.updateGoalCompletion(goal, player.getUUID());
                    }

                    lockout.mostUniqueCraftsPlayer = player.getUUID();
                    lockout.mostUniqueCrafts = crafts.size();
                }
                // Send tooltip updates to all teams whenever anyone makes progress
                for (LockoutTeam teamToUpdate : lockout.getTeams()) {
                    ((LockoutTeamServer) teamToUpdate).sendTooltipUpdate((HaveMostUniqueCraftsGoal) goal, true);
                }
            }
        }
    }

}
