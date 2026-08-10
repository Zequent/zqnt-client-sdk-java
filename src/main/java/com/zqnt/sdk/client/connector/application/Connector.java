package com.zqnt.sdk.client.connector.application;

import com.zqnt.sdk.client.connector.domains.*;
import com.zqnt.sdk.client.livedata.domains.StreamHandle;
import com.zqnt.sdk.client.missionautonomy.domains.SchedulerResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Access to all endpoints exposed by the Connector gRPC service. */
public interface Connector {

    CompletableFuture<ConnectorResponse> registerAsset(RegisterAssetRequest request);
    CompletableFuture<ConnectorResponse> deregisterAsset(DeregisterAssetRequest request);
    CompletableFuture<ConnectorResponse> updateAsset(UpdateAssetRequest request);
    CompletableFuture<ConnectorResponse> updateSubAsset(UpdateSubAssetRequest request);
    CompletableFuture<ConnectorResponse> getAssetBySn(GetAssetBySnRequest request);
    CompletableFuture<ConnectorResponse> getAssetById(GetAssetByIdRequest request);
    CompletableFuture<ConnectorResponse> getSubAssetBySn(GetSubAssetBySnRequest request);

    CompletableFuture<AssetPayloadResponse> upsertAssetPayload(UpsertAssetPayloadRequest request);
    CompletableFuture<AssetPayloadListResponse> listAssetPayloads(ListAssetPayloadsRequest request);
    CompletableFuture<AssetPayloadResponse> deleteAssetPayload(DeleteAssetPayloadRequest request);

    CompletableFuture<ConnectorResponse> getOrganization(GetOrganizationRequest request);

    CompletableFuture<SchedulerResponse> getScheduler(GetSchedulerRequest request);
    CompletableFuture<SchedulerResponse> createScheduler(CreateSchedulerRequest request);
    CompletableFuture<SchedulerResponse> createSchedulers(CreateSchedulersRequest request);
    CompletableFuture<SchedulerResponse> updateScheduler(UpdateSchedulerRequest request);
    CompletableFuture<SchedulerResponse> deleteScheduler(DeleteSchedulerRequest request);
    CompletableFuture<SchedulerResponse> deleteSchedulers(DeleteSchedulersRequest request);

    CompletableFuture<ConnectorPolicyResponse> getActivePoliciesByType(GetPoliciesRequest request);
    CompletableFuture<ConnectorPolicyResponse> getAllActivePolicies(GetAllActivePoliciesRequest request);
    CompletableFuture<ConnectorConfigResponse> getTechnicalConfigs(GetTechnicalConfigsRequest request);

    StreamHandle assetMonitoring(AssetMonitoringRequest request,
                                 Consumer<AssetMonitoringResponse> onData,
                                 Consumer<Throwable> onError);

    ConnectorBatchSession<StoreTelemetryRequest> storeTelemetryBatch();
    ConnectorBatchSession<StoreDetectionRequest> storeDetectionBatch();
    ConnectorBatchSession<StoreNotificationRequest> storeNotificationBatch();
}
