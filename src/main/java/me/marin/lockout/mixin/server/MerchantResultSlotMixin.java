package me.marin.lockout.mixin.server;

import me.marin.lockout.Lockout;
import me.marin.lockout.LockoutTeamServer;
import me.marin.lockout.lockout.Goal;
import me.marin.lockout.lockout.interfaces.TradeWithXUniqueProfessionsGoal;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(MerchantResultSlot.class)
public class MerchantResultSlotMixin {

    @Shadow @Final
    private Merchant merchant;

    @Shadow @Final
    private MerchantContainer slots;

    @Inject(method = "onTake", at = @At("TAIL"))
    private void lockout$onTrade(Player player, ItemStack carried, CallbackInfo ci) {
        if (player.level().isClientSide()) return;
        
        Lockout lockout = LockoutServer.lockout;
        if (!Lockout.isLockoutRunning(lockout)) return;

        // Get the offer that was just traded
        MerchantOffer offer = this.slots.getActiveOffer();
        if (offer == null) return;

        // Check if merchant is a villager
        if (!(merchant instanceof Villager villager)) return;

        VillagerProfession profession = villager.getVillagerData().profession().value();
        
        // Track per-player for statistics
        lockout.playerTradedProfessions.computeIfAbsent(player.getUUID(), p -> new LinkedHashSet<>());
        lockout.playerTradedProfessions.get(player.getUUID()).add(profession);

        for (Goal goal : lockout.getBoard().getGoals()) {
            if (goal == null) continue;
            if (goal.isCompleted()) continue;

            if (goal instanceof TradeWithXUniqueProfessionsGoal tradeGoal) {
                LockoutTeamServer team = (LockoutTeamServer) lockout.getPlayerTeam(player.getUUID());
                if (team == null) continue;

                tradeGoal.getTrackerMap().computeIfAbsent(team, t -> new LinkedHashSet<>());
                boolean newProfession = tradeGoal.getTrackerMap().get(team).add(profession);
                
                // Track first contributor
                if (newProfession) {
                    lockout.firstTradedProfessionContributor.putIfAbsent(team, new HashMap<>());
                    lockout.firstTradedProfessionContributor.get(team).put(profession, player.getUUID());
                }

                int size = tradeGoal.getTrackerMap().get(team).size();

                team.sendTooltipUpdate(tradeGoal);
                if (size >= tradeGoal.getAmount()) {
                    lockout.completeGoal(tradeGoal, team);
                }
            }
        }
    }
}
