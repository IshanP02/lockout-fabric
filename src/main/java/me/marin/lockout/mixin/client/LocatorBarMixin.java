package me.marin.lockout.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.datafixers.util.Either;
import me.marin.lockout.LockoutTeam;
import me.marin.lockout.client.LockoutClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.client.world.ClientWaypointHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.EntityTickProgress;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.Waypoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;
import java.util.UUID;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin implements Bar {

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private static Identifier ARROW_UP;
    @Shadow @Final private static Identifier ARROW_DOWN;

    @Redirect(
            method = "renderAddons",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWaypointHandler;forEachWaypoint(Lnet/minecraft/entity/Entity;Ljava/util/function/Consumer;)V")
    )
    private void lockout$renderHeads(ClientWaypointHandler instance, Entity entity, Consumer<TrackedWaypoint> originalAction, DrawContext context, RenderTickCounter tickCounter) {
        World world = entity.getEntityWorld();
        int centerY = getCenterY(this.client.getWindow());
        EntityTickProgress entityTickProgress = e -> tickCounter.getTickProgress(!world.getTickManager().shouldSkipTick(e));

        instance.forEachWaypoint(entity, waypoint -> {
            // Original filtering logic
            if ((Boolean) waypoint.getSource().left().map(uuid -> uuid.equals(entity.getUuid())).orElse(false)) {
                return;
            }

            double yaw = waypoint.getRelativeYaw(world, (TrackedWaypoint.YawProvider) this.client.gameRenderer.getCamera(), entityTickProgress);
            if (yaw <= -60.0 || yaw > 60.0) {
                return;
            }

            int centerX = MathHelper.ceil((this.client.getWindow().getScaledWidth() - 9) / 2.0F);
            int offset = MathHelper.floor(yaw * 173.0 / 2.0 / 60.0);
            int x = centerX + offset;
            int size = 7; 
            int y = centerY - 1; 

            Waypoint.Config config = waypoint.getConfig();
            Either<UUID, String> source = waypoint.getSource();

            boolean renderedHead = false;

            if (source.left().isPresent()) {
                UUID uuid = source.left().get();
                PlayerListEntry entry = this.client.getNetworkHandler() != null ? this.client.getNetworkHandler().getPlayerListEntry(uuid) : null;

                if (entry != null) {
                    // Get team color from scoreboard (not from Lockout.getPlayerTeam which causes ClassCastException)
                    int teamColor = -1;
                    String playerName = entry.getProfile().name();
                    if (playerName != null && this.client.world != null) {
                        var scoreboard = this.client.world.getScoreboard();
                        var team = scoreboard.getScoreHolderTeam(playerName);
                        if (team != null && team.getColor() != null) {
                            Integer val = team.getColor().getColorValue();
                            if (val != null) teamColor = val | 0xFF000000;
                        }
                    }

                    SkinTextures textures = entry.getSkinTextures();

                    // Render border if team color exists
                    if (teamColor != -1) {
                        int bt = 1; // Border thickness
                        context.fill(RenderPipelines.GUI, x - bt, y - bt, x + size + bt, y, teamColor); // top
                        context.fill(RenderPipelines.GUI, x - bt, y + size, x + size + bt, y + size + bt, teamColor); // bottom
                        context.fill(RenderPipelines.GUI, x - bt, y, x, y + size, teamColor); // left
                        context.fill(RenderPipelines.GUI, x + size, y, x + size + bt, y + size, teamColor); // right
                    }

                    // Use PlayerSkinDrawer for proper head rendering
                    net.minecraft.client.gui.PlayerSkinDrawer.draw(context, textures, x, y, size);
                    renderedHead = true;
                }
            }

            if (!renderedHead) {
                // Fallback to original sprite rendering
                WaypointStyleAsset style = this.client.getWaypointStyleAssetManager().get(config.style);
                float dist = MathHelper.sqrt((float) waypoint.squaredDistanceTo(entity));
                Identifier identifier = style.getSpriteForDistance(dist);
                int color = config.color.orElseGet(() -> 0xFFFFFF);

                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, x, y, size, size, color);
            }

            // Original pitch/arrow logic
            TrackedWaypoint.Pitch pitch = waypoint.getPitch(world, (TrackedWaypoint.PitchProvider) this.client.gameRenderer, entityTickProgress);
            if (pitch != TrackedWaypoint.Pitch.NONE) {
                int arrowY;
                Identifier arrowSprite;
                if (pitch == TrackedWaypoint.Pitch.DOWN) {
                    arrowY = 6;
                    arrowSprite = ARROW_DOWN;
                } else {
                    arrowY = -6;
                    arrowSprite = ARROW_UP;
                }
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, arrowSprite, x + 1, centerY + arrowY, 7, 5);
            }
        });
    }
}