package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.goals.have_more.HaveMostUniqueSmeltsGoal;
import me.marin.lockout.lockout.goals.workstation.LockMapUsingCartographyTableGoal;
import me.marin.lockout.lockout.goals.workstation.UseLoomGoal;
import me.marin.lockout.lockout.goals.workstation.UseStonecutterGoal;
import me.marin.lockout.lockout.interfaces.FurnaceSmeltTracker;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Set;

@Mixin(Slot.class)
public class SlotMixin {

    @Inject(method="onTakeItem", at = @At("HEAD"))
    public void onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getEntityWorld().isClient()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        Slot slot = (Slot) (Object) this;

        // Handle furnace output slot for unique smelts tracking
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (slot.inventory instanceof AbstractFurnaceBlockEntity furnace) {
                if (furnace instanceof FurnaceSmeltTracker tracker) {
                    Item inputItem = tracker.lockout$getLastSmelted();
                    if (inputItem != null) {
                        lockout.uniqueSmelts.putIfAbsent(serverPlayer.getUuid(), new java.util.HashSet<>());
                        Set<Item> smelts = lockout.uniqueSmelts.get(serverPlayer.getUuid());
                        boolean addedNew = smelts.add(inputItem);

                        if (addedNew) {
                            for (Goal goal : lockout.getBoard().getGoals()) {
                                if (goal == null) continue;

                                if (goal instanceof HaveMostUniqueSmeltsGoal) {
                                    // Play sound only to this specific player (client-side)
                                    RegistryEntry<net.minecraft.sound.SoundEvent> soundEntry = SoundEvents.BLOCK_NOTE_BLOCK_PLING;
                                    serverPlayer.networkHandler.sendPacket(
                                        new PlaySoundS2CPacket(
                                            soundEntry,
                                            SoundCategory.MASTER,
                                            serverPlayer.getX(),
                                            serverPlayer.getY(),
                                            serverPlayer.getZ(),
                                            2f,
                                            1.5f,
                                            serverPlayer.getEntityWorld().random.nextLong()
                                        )
                                    );
                                    
                                    if (smelts.size() % 5 == 0) {
                                        serverPlayer.sendMessage(Text.of(Formatting.GRAY + "" + Formatting.ITALIC + "You have smelted " + smelts.size() + " unique items."), false);
                                    }
                                    serverPlayer.sendMessage(Text.of("Unique smelts: " + smelts.size()), true);

                                    if (smelts.size() > lockout.mostUniqueSmelts) {
                                        if (!Objects.equals(lockout.mostUniqueSmeltsPlayer, serverPlayer.getUuid())) {
                                            lockout.updateGoalCompletion(goal, serverPlayer.getUuid());
                                        }

                                        lockout.mostUniqueSmeltsPlayer = serverPlayer.getUuid();
                                        lockout.mostUniqueSmelts = smelts.size();
                                        // Send tooltip updates to all teams
                                        for (LockoutTeam teamToUpdate : lockout.getTeams()) {
                                            ((LockoutTeamServer) teamToUpdate).sendTooltipUpdate((HaveMostUniqueSmeltsGoal) goal, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof UseStonecutterGoal) {
                if (player.currentScreenHandler instanceof StonecutterScreenHandler stonecutterScreenHandler) {
                    if ((Object) this == stonecutterScreenHandler.slots.get(1)) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }

            if (goal instanceof UseLoomGoal) {
                if (player.currentScreenHandler instanceof LoomScreenHandler loomScreenHandler) {
                    if ((Object) this == loomScreenHandler.getOutputSlot()) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }

            if (goal instanceof LockMapUsingCartographyTableGoal) {
                // Check if the taken item is a filled map
                if (stack.getItem() == Items.FILLED_MAP) {
                    // Get the map ID from the item
                    MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
                    if (mapIdComponent != null) {
                        // Get the MapState from the server
                        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();
                        MapState mapState = serverWorld.getMapState(mapIdComponent);
                        
                        // Check if the map is locked
                        if (mapState != null && mapState.locked) {
                            lockout.completeGoal(goal, player);
                        }
                    }
                }
            }

        }
    }

}
