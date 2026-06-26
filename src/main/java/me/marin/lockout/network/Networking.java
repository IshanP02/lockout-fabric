package me.marin.lockout.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Networking {
    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(UpdateTimerPayload.ID, UpdateTimerPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdateTooltipPayload.ID, UpdateTooltipPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LockoutGoalsTeamsPayload.ID, LockoutGoalsTeamsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StartLockoutPayload.ID, StartLockoutPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompleteTaskPayload.ID, CompleteTaskPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EndLockoutPayload.ID, EndLockoutPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LockoutVersionPayload.ID, LockoutVersionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdatePicksBansPayload.ID, UpdatePicksBansPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BroadcastPickBanPayload.ID, BroadcastPickBanPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncPickBanLimitPayload.ID, SyncPickBanLimitPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SetBoardTypePayload.ID, SetBoardTypePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StartPickBanSessionPayload.ID, StartPickBanSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdatePickBanSessionPayload.ID, UpdatePickBanSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EndPickBanSessionPayload.ID, EndPickBanSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncLocateDataPayload.ID, SyncLocateDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GoalDetailsPayload.ID, GoalDetailsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DownloadStatisticsPayload.ID, DownloadStatisticsPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(CustomBoardPayload.ID, CustomBoardPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LockoutVersionPayload.ID, LockoutVersionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdatePicksBansPayload.ID, UpdatePicksBansPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BroadcastPickBanPayload.ID, BroadcastPickBanPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SyncPickBanLimitPayload.ID, SyncPickBanLimitPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LockPickBanSelectionsPayload.ID, LockPickBanSelectionsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UploadBoardTypePayload.ID, UploadBoardTypePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AnnounceGoalFocusPayload.ID, AnnounceGoalFocusPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestGoalDetailsPayload.ID, RequestGoalDetailsPayload.CODEC);
    }
}
