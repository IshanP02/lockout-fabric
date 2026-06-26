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
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Set;

@Mixin(Slot.class)
public class SlotMixin {

    @Inject(method="onTake", at = @At("HEAD"))
    public void onTakeItem(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;
        if (!lockout.isLockoutPlayer(player.getUUID())) return;

        Slot slot = (Slot) (Object) this;

        // Handle furnace output slot for unique smelts tracking
        if (player instanceof ServerPlayer serverPlayer) {
            if (slot.container instanceof AbstractFurnaceBlockEntity furnace) {
                if (furnace instanceof FurnaceSmeltTracker tracker) {
                    Item inputItem = tracker.lockout$getLastSmelted();
                    if (inputItem != null) {
                        lockout.uniqueSmelts.putIfAbsent(serverPlayer.getUUID(), new java.util.HashSet<>());
                        Set<Item> smelts = lockout.uniqueSmelts.get(serverPlayer.getUUID());
                        boolean addedNew = smelts.add(inputItem);

                        if (addedNew) {
                            for (Goal goal : lockout.getBoard().getGoals()) {
                                if (goal == null) continue;

                                if (goal instanceof HaveMostUniqueSmeltsGoal) {
                                    // Play sound only to this specific player (client-side)
                                    Holder<net.minecraft.sounds.SoundEvent> soundEntry = SoundEvents.NOTE_BLOCK_PLING;
                                    serverPlayer.connection.send(
                                        new ClientboundSoundPacket(
                                            soundEntry,
                                            SoundSource.MASTER,
                                            serverPlayer.getX(),
                                            serverPlayer.getY(),
                                            serverPlayer.getZ(),
                                            2f,
                                            1.5f,
                                            serverPlayer.level().getRandom().nextLong()
                                        )
                                    );
                                    
                                    if (smelts.size() % 5 == 0) {
                                        serverPlayer.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "" + ChatFormatting.ITALIC + "You have smelted " + smelts.size() + " unique items."));
                                    }
                                    serverPlayer.sendSystemMessage(Component.literal("Unique smelts: " + smelts.size()));

                                    if (smelts.size() > lockout.mostUniqueSmelts) {
                                        if (!Objects.equals(lockout.mostUniqueSmeltsPlayer, serverPlayer.getUUID())) {
                                            lockout.updateGoalCompletion(goal, serverPlayer.getUUID());
                                        }

                                        lockout.mostUniqueSmeltsPlayer = serverPlayer.getUUID();
                                        lockout.mostUniqueSmelts = smelts.size();
                                    }
                                    // Send tooltip updates to all teams whenever anyone makes progress
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

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof UseStonecutterGoal) {
                if (player.containerMenu instanceof StonecutterMenu stonecutterScreenHandler) {
                    if ((Object) this == stonecutterScreenHandler.slots.get(1)) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }

            if (goal instanceof UseLoomGoal) {
                if (player.containerMenu instanceof LoomMenu loomScreenHandler) {
                    if ((Object) this == loomScreenHandler.slots.get(3)) {
                        lockout.completeGoal(goal, player);
                    }
                }
            }

            if (goal instanceof LockMapUsingCartographyTableGoal) {
                // Check if the taken item is a filled map
                if (stack.getItem() == Items.FILLED_MAP) {
                    // Get the map ID from the item
                    MapId MapId = stack.get(DataComponents.MAP_ID);
                    if (MapId != null) {
                        // Get the MapItemSavedData from the server
                        ServerLevel serverWorld = (ServerLevel) player.level();
                        MapItemSavedData mapState = serverWorld.getMapData(MapId);
                        
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
