package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.have_more.HaveMostUniqueCraftsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    @Shadow @Final
    private PlayerEntity player;

    @Shadow
    private int amount;

    @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    public void onCraft(ItemStack stack, CallbackInfo ci) {
        if (player.getEntityWorld().isClient()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        if (amount < 0 || stack.isEmpty()) {
            return;
        }

        if (!(player.currentScreenHandler instanceof CraftingScreenHandler || player.currentScreenHandler instanceof PlayerScreenHandler)) return;

        lockout.uniqueCrafts.putIfAbsent(player.getUuid(), new HashSet<>());
        Set<Item> crafts = lockout.uniqueCrafts.get(player.getUuid());
        boolean addedNew = crafts.add(stack.getItem());

        if (!addedNew) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;

            if (goal instanceof HaveMostUniqueCraftsGoal) {
                // Play sound only to this specific player (client-side)
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    RegistryEntry<net.minecraft.sound.SoundEvent> soundEntry = SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
                    serverPlayer.networkHandler.sendPacket(
                        new PlaySoundS2CPacket(
                            soundEntry,
                            SoundCategory.MASTER,
                            serverPlayer.getX(),
                            serverPlayer.getY(),
                            serverPlayer.getZ(),
                            2f,
                            2f,
                            player.getEntityWorld().random.nextLong()
                        )
                    );
                }
                if (crafts.size() % 5 == 0) {
                    player.sendMessage(Text.of(Formatting.GRAY + "" + Formatting.ITALIC + "You have crafted " + crafts.size() + " unique items."), false);
                }
                player.sendMessage(Text.of("Unique crafts: " + crafts.size()), true);

                if (crafts.size() > lockout.mostUniqueCrafts) {
                    if (!Objects.equals(lockout.mostUniqueCraftsPlayer, player.getUuid())) {
                        lockout.updateGoalCompletion(goal, player.getUuid());
                    }

                    lockout.mostUniqueCraftsPlayer = player.getUuid();
                    lockout.mostUniqueCrafts = crafts.size();
                    // Send tooltip updates to all teams
                    for (LockoutTeam teamToUpdate : lockout.getTeams()) {
                        ((LockoutTeamServer) teamToUpdate).sendTooltipUpdate((HaveMostUniqueCraftsGoal) goal, true);
                    }
                }
            }
        }
    }

}
