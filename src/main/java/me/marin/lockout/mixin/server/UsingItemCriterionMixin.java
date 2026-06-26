package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.interfaces.LookAtMobGoal;
import me.marin.lockout.lockout.interfaces.LookAtUniqueMobsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancements.triggers.UsingItemTrigger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Mixin(UsingItemTrigger.class)
public class UsingItemCriterionMixin {

    @Inject(method = "trigger", at = @At("TAIL"))
    private void lockout$onUsingItem(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() != Items.SPYGLASS) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        // Raycast to find entity being looked at (similar to spyglass advancements)
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(100.0));

        // Find entities in the raycast path
        AABB searchBox = new AABB(start, end);
        List<Entity> entities = player.level().getEntities(player, searchBox,
            e -> e instanceof LivingEntity && !e.isSpectator());

        // Find the closest entity in the look direction
        Optional<Entity> closestEntity = entities.stream()
            .filter(e -> {
                Vec3 toEntity = e.position().subtract(start);
                double dotProduct = toEntity.normalize().dot(direction);
                return dotProduct > 0.99; // Very narrow cone (spyglass FOV)
            })
            .min((e1, e2) -> {
                double dist1 = e1.distanceToSqr(start);
                double dist2 = e2.distanceToSqr(start);
                return Double.compare(dist1, dist2);
            });

        if (closestEntity.isEmpty()) return;

        Entity entity = closestEntity.get();
        if (!(entity instanceof LivingEntity)) return;

        // Check line of sight - ensure no solid blocks between player and entity
        Vec3 entityEyePos = entity.position().add(0, entity.getEyeHeight(entity.getPose()), 0);
        ClipContext raycastContext = new ClipContext(
            start,
            entityEyePos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        );
        BlockHitResult blockHitResult = player.level().clip(raycastContext);

        // If we hit a block before reaching the entity, they're not in line of sight
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            double distanceToBlock = start.distanceToSqr(blockHitResult.getLocation());
            double distanceToEntity = start.distanceToSqr(entityEyePos);
            if (distanceToBlock < distanceToEntity) {
                return; // Block is in the way
            }
        }

        LockoutTeam team = lockout.getPlayerTeam(player.getUUID());
        if (team == null) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof LookAtUniqueMobsGoal lookAtGoal) {
                lockout.lookedAtMobTypes.computeIfAbsent(team, t -> new LinkedHashSet<>());
                boolean newMob = lockout.lookedAtMobTypes.get(team).add(entity.getType());

                // Track per-player for statistics
                lockout.playerLookedAtMobs.computeIfAbsent(player.getUUID(), p -> new LinkedHashSet<>());
                lockout.playerLookedAtMobs.get(player.getUUID()).add(entity.getType());
                
                // Track first contributor
                if (newMob) {
                    lockout.firstLookedAtMobContributor.putIfAbsent(team, new HashMap<>());
                    lockout.firstLookedAtMobContributor.get(team).put(entity.getType(), player.getUUID());

                    Holder<net.minecraft.sounds.SoundEvent> soundEntry = SoundEvents.NOTE_BLOCK_CHIME;
                    player.connection.send(
                        new ClientboundSoundPacket(
                            soundEntry,
                            SoundSource.MASTER,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            2f,
                            2f,
                            player.level().getRandom().nextLong()
                        )
                    );

                    int size = lockout.lookedAtMobTypes.get(team).size();

                    // Display count above action bar
                    player.sendSystemMessage(Component.literal("Mobs Looked at: " + size), true);
                }

                int size = lockout.lookedAtMobTypes.get(team).size();

                ((LockoutTeamServer) team).sendTooltipUpdate((Goal & HasTooltipInfo) goal);
                if (size >= lookAtGoal.getAmount()) {
                    lockout.completeGoal(lookAtGoal, team);
                }
            }
            
            if (goal instanceof LookAtMobGoal lookAtMobGoal) {
                if (lookAtMobGoal.matchesMob(entity)) {
                    lockout.completeGoal(lookAtMobGoal, player);
                }
            }
        }
    }
}
