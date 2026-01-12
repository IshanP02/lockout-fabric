package me.marin.lockout.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Networking {
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(UpdateTimerPayload.ID, UpdateTimerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateTooltipPayload.ID, UpdateTooltipPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LockoutGoalsTeamsPayload.ID, LockoutGoalsTeamsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartLockoutPayload.ID, StartLockoutPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CompleteTaskPayload.ID, CompleteTaskPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EndLockoutPayload.ID, EndLockoutPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LockoutVersionPayload.ID, LockoutVersionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdatePicksBansPayload.ID, UpdatePicksBansPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BroadcastPickBanPayload.ID, BroadcastPickBanPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPickBanLimitPayload.ID, SyncPickBanLimitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SetBoardTypePayload.ID, SetBoardTypePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StartPickBanSessionPayload.ID, StartPickBanSessionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdatePickBanSessionPayload.ID, UpdatePickBanSessionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EndPickBanSessionPayload.ID, EndPickBanSessionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncLocateDataPayload.ID, SyncLocateDataPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(CustomBoardPayload.ID, CustomBoardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LockoutVersionPayload.ID, LockoutVersionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePicksBansPayload.ID, UpdatePicksBansPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BroadcastPickBanPayload.ID, BroadcastPickBanPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SyncPickBanLimitPayload.ID, SyncPickBanLimitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LockPickBanSelectionsPayload.ID, LockPickBanSelectionsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadBoardTypePayload.ID, UploadBoardTypePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AnnounceGoalFocusPayload.ID, AnnounceGoalFocusPayload.CODEC);
    }
}
