package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.HasTooltipInfo;
import me.marin.lockout.lockout.interfaces.LookAtMobGoal;
import me.marin.lockout.lockout.interfaces.LookAtUniqueMobsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.advancement.criterion.UsingItemCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Mixin(UsingItemCriterion.class)
public class UsingItemCriterionMixin {

    @Inject(method = "trigger", at = @At("TAIL"))
    private void lockout$onUsingItem(ServerPlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() != Items.SPYGLASS) return;

        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        // Raycast to find entity being looked at (similar to spyglass advancements)
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(100.0));

        // Find entities in the raycast path
        Box searchBox = new Box(start, end);
        List<Entity> entities = player.getEntityWorld().getOtherEntities(player, searchBox, 
            e -> e instanceof LivingEntity && !e.isSpectator());

        // Find the closest entity in the look direction
        Optional<Entity> closestEntity = entities.stream()
            .filter(e -> {
                Vec3d toEntity = e.getEntityPos().subtract(start);
                double dotProduct = toEntity.normalize().dotProduct(direction);
                return dotProduct > 0.99; // Very narrow cone (spyglass FOV)
            })
            .min((e1, e2) -> {
                double dist1 = e1.squaredDistanceTo(start);
                double dist2 = e2.squaredDistanceTo(start);
                return Double.compare(dist1, dist2);
            });

        if (closestEntity.isEmpty()) return;
        
        Entity entity = closestEntity.get();
        if (!(entity instanceof LivingEntity)) return;

        // Check line of sight - ensure no solid blocks between player and entity
        Vec3d entityEyePos = entity.getEntityPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
        RaycastContext raycastContext = new RaycastContext(
            start, 
            entityEyePos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        );
        BlockHitResult blockHitResult = player.getEntityWorld().raycast(raycastContext);
        
        // If we hit a block before reaching the entity, they're not in line of sight
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            double distanceToBlock = start.squaredDistanceTo(blockHitResult.getPos());
            double distanceToEntity = start.squaredDistanceTo(entityEyePos);
            if (distanceToBlock < distanceToEntity) {
                return; // Block is in the way
            }
        }

        LockoutTeam team = lockout.getPlayerTeam(player.getUuid());
        if (team == null) return;

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof LookAtUniqueMobsGoal lookAtGoal) {
                lockout.lookedAtMobTypes.computeIfAbsent(team, t -> new LinkedHashSet<>());
                boolean newMob = lockout.lookedAtMobTypes.get(team).add(entity.getType());

                // Track per-player for statistics
                lockout.playerLookedAtMobs.computeIfAbsent(player.getUuid(), p -> new LinkedHashSet<>());
                lockout.playerLookedAtMobs.get(player.getUuid()).add(entity.getType());
                
                // Track first contributor
                if (newMob) {
                    lockout.firstLookedAtMobContributor.putIfAbsent(team, new HashMap<>());
                    lockout.firstLookedAtMobContributor.get(team).put(entity.getType(), player.getUuid());
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
