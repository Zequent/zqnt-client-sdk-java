package com.zqnt.sdk.client.missionautonomy.application;

import com.zqnt.sdk.client.missionautonomy.domains.SchedulerResponse;
import com.zqnt.sdk.client.missionautonomy.domains.MissionResponse;
import com.zqnt.sdk.client.missionautonomy.domains.TaskResponse;
import com.zqnt.sdk.client.missionautonomy.capabilities.*;
import com.zqnt.utils.execution.proto.CapabilityExecutionProtoDTO;
import com.zqnt.utils.execution.proto.CapabilityPackageProtoDTO;
import com.zqnt.utils.execution.proto.ResolvedExecutionConfigProtoDTO;
import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import com.zqnt.utils.missionautonomy.domains.MissionZoneDTO;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MissionAutonomy {

    // Capability package administration
    CompletableFuture<CapabilityPackageProtoDTO> upsertCapabilityPackage(
            CapabilityPackageProtoDTO capabilityPackage, String expectedRevision);
    CompletableFuture<CapabilityPackageProtoDTO> getCapabilityPackage(String packageId, String version);
    CompletableFuture<CapabilityPage<CapabilityPackageProtoDTO>> listCapabilityPackages(CapabilityPackageQuery query);
    CompletableFuture<Void> deleteCapabilityPackage(String packageId, String version,
                                                     String expectedRevision);

    // Mission-free capability execution
    CompletableFuture<CapabilityExecutionProtoDTO> createCapabilityExecution(CapabilityExecutionCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> executeCapability(CapabilityExecutionCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> getCapabilityExecution(String executionId);
    CompletableFuture<CapabilityPage<CapabilityExecutionProtoDTO>> listCapabilityExecutions(
            CapabilityExecutionQuery query);
    CompletableFuture<CapabilityExecutionProtoDTO> startCapabilityExecution(CapabilityLifecycleCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> pauseCapabilityExecution(CapabilityLifecycleCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> resumeCapabilityExecution(CapabilityLifecycleCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> cancelCapabilityExecution(CapabilityLifecycleCommand command);
    CompletableFuture<CapabilityExecutionProtoDTO> signalCapabilityExecution(CapabilitySignalCommand command);
    CompletableFuture<ResolvedExecutionConfigProtoDTO> resolveExecutionConfig(ExecutionConfigQuery query);

    /** @deprecated Missions were replaced by capability executions. */
    @Deprecated CompletableFuture<MissionResponse> createMission(MissionDTO value);
    /** @deprecated Missions were replaced by capability executions. */
    @Deprecated CompletableFuture<MissionResponse> updateMission(String id, MissionDTO value);
    /** @deprecated Missions were replaced by capability executions. */
    @Deprecated CompletableFuture<MissionResponse> getMission(String id);
    /** @deprecated Missions were replaced by capability executions. */
    @Deprecated CompletableFuture<MissionResponse> deleteMission(String id);
    /** @deprecated Missions were replaced by capability executions. */
    @Deprecated CompletableFuture<MissionResponse> uploadMissionNfzZones(String id, List<MissionZoneDTO> zones,
                                                                          boolean replaceExisting);
    /** @deprecated Tasks were replaced by capability execution nodes. */
    @Deprecated CompletableFuture<TaskResponse> createTask(TaskDTO value);
    /** @deprecated Tasks were replaced by capability execution nodes. */
    @Deprecated CompletableFuture<TaskResponse> updateTask(String id, TaskDTO value);
    /** @deprecated Tasks were replaced by capability execution nodes. */
    @Deprecated CompletableFuture<TaskResponse> getTask(String id);
    /** @deprecated Tasks were replaced by capability execution nodes. */
    @Deprecated CompletableFuture<TaskResponse> getTaskByFlightId(String id);
    /** @deprecated Tasks were replaced by capability execution nodes. */
    @Deprecated CompletableFuture<TaskResponse> deleteTask(String id);
    /** @deprecated Tasks were replaced by capability execution lifecycle operations. */
    @Deprecated CompletableFuture<TaskResponse> startTask(String id);
    /** @deprecated Tasks were replaced by capability execution lifecycle operations. */
    @Deprecated CompletableFuture<TaskResponse> stopTask(String id);
    /** @deprecated Tasks were replaced by capability execution lifecycle operations. */
    @Deprecated CompletableFuture<TaskResponse> pauseTask(String id);
    /** @deprecated Tasks were replaced by capability execution lifecycle operations. */
    @Deprecated CompletableFuture<TaskResponse> resumeTask(String id);

    // Schedule
    CompletableFuture<SchedulerResponse> createScheduler(SchedulerDTO request);
    CompletableFuture<SchedulerResponse> updateScheduler(String schedulerId, SchedulerDTO schedulerDTO);
    CompletableFuture<SchedulerResponse> getScheduler(String schedulerId);
    CompletableFuture<SchedulerResponse> deleteScheduler(String schedulerId);
    CompletableFuture<SchedulerResponse> createSchedulers(List<SchedulerDTO> schedulerDTOS);
    CompletableFuture<SchedulerResponse> deleteSchedulers(List<String> schedulerIds);
}
