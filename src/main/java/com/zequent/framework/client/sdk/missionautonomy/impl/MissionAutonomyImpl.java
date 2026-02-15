package com.zequent.framework.client.sdk.missionautonomy.impl;

import com.zequent.framework.client.sdk.config.GrpcClientConfig;
import com.zequent.framework.client.sdk.missionautonomy.MissionAutonomy;
import com.zequent.framework.client.sdk.models.MissionResponse;
import com.zequent.framework.client.sdk.models.TaskResponse;
import com.zequent.framework.client.sdk.models.SchedulerResponse;
import com.zequent.framework.client.sdk.resilience.GrpcResilience;
import com.zequent.framework.common.proto.RequestBase;
import com.zequent.framework.services.mission.proto.*;
import com.zequent.framework.utils.core.ProtobufHelpers;
import com.zequent.framework.utils.missionautonomy.dto.MissionDTO;
import com.zequent.framework.utils.missionautonomy.dto.SchedulerDTO;
import com.zequent.framework.utils.missionautonomy.dto.TaskDTO;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MissionAutonomyImpl implements MissionAutonomy {

    private final MutinyMissionAutonomyServiceGrpc.MutinyMissionAutonomyServiceStub missionAutonomyService;
    private final GrpcResilience resilience;
    private final GrpcClientConfig config;

    /**
     * Private constructor - use create() factory method.
     */
    private MissionAutonomyImpl(GrpcClientConfig config, ManagedChannel channel) {
        this.config = config;
        this.resilience = new GrpcResilience(
                config.getMaxRetryAttempts(),
                config.getRetryDelayMillis(),
                config.getCircuitBreakerFailureThreshold(),
                config.getCircuitBreakerWaitDurationMillis()
        );
        this.missionAutonomyService = MutinyMissionAutonomyServiceGrpc.newMutinyStub(channel);
        log.debug("MissionAutonomy created with channel for {}:{}",
                config.getMissionAutonomyConfig().getHost(),
                config.getMissionAutonomyConfig().getPort());
    }

    public static MissionAutonomy create(GrpcClientConfig config, ManagedChannel channel) {
        return new MissionAutonomyImpl(config, channel);
    }


    @Override
    public CompletableFuture<MissionResponse> createMission(MissionDTO missionDTO) {
        log.info("Creating mission: name={}", missionDTO.getName());

        var missionBuilder = com.zequent.framework.common.proto.MissionProtoDTO.newBuilder()
                .setStatus(missionDTO.getStatus())
                .setType(missionDTO.getType())
                .setDescription(missionDTO.getDescription() != null ? missionDTO.getDescription() : "")
                .setName(missionDTO.getName() != null ? missionDTO.getName() : "")
                .setStartDate(missionDTO.getStartDate() != null ? ProtobufHelpers.toTimestamp(missionDTO.getStartDate()) : null)
                .setEndDate(missionDTO.getEndDate() != null ? ProtobufHelpers.toTimestamp(missionDTO.getEndDate()) : null);

        if (missionDTO.getAssignedAssets() != null) {
            missionBuilder.addAllAssignedAssets(missionDTO.getAssignedAssets());
        }

        var protoRequest = CreateMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionDTO(missionBuilder.build())
                .build();

        return executeAsync(missionAutonomyService.createMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<MissionResponse> updateMission(String missionId, MissionDTO missionDTO) {
        log.info("Updating mission: missionId={}", missionId);

        var missionBuilder = com.zequent.framework.common.proto.MissionProtoDTO.newBuilder()
                .setId(missionId)
                .setStatus(missionDTO.getStatus())
                .setType(missionDTO.getType())
                .setDescription(missionDTO.getDescription() != null ? missionDTO.getDescription() : "")
                .setName(missionDTO.getName() != null ? missionDTO.getName() : "")
                .setStartDate(missionDTO.getStartDate() != null ? ProtobufHelpers.toTimestamp(missionDTO.getStartDate()) : null)
                .setEndDate(missionDTO.getEndDate() != null ? ProtobufHelpers.toTimestamp(missionDTO.getEndDate()) : null);

        if (missionDTO.getAssignedAssets() != null) {
            missionBuilder.addAllAssignedAssets(missionDTO.getAssignedAssets());
        }

        var protoRequest = UpdateMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .setMissionDTO(missionBuilder.build())
                .build();

        return executeAsync(missionAutonomyService.updateMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<MissionResponse> getMission(String missionId) {
        log.info("Getting mission: missionId={}", missionId);

        var protoRequest = GetMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .build();

        return executeAsync(missionAutonomyService.getMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<MissionResponse> deleteMission(String missionId) {
        log.info("Deleting mission: missionId={}", missionId);

        var protoRequest = DeleteMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .build();

        return executeAsync(missionAutonomyService.deleteMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> createTask(TaskDTO taskDTO) {
        log.info("Creating task: name={}", taskDTO.getName());

        var protoRequest = CreateTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskDTO(com.zequent.framework.common.proto.TaskProtoDTO.newBuilder()
                        .setName(taskDTO.getName())
                        .setFlightId(taskDTO.getFlightId() != null ? taskDTO.getFlightId() : "")
                        .setSnNumber(taskDTO.getSnNumber())
                        .setMissionId(taskDTO.getMissionId().toString())
                        .build())
                .build();

        return executeAsync(missionAutonomyService.createTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> updateTask(String taskId, TaskDTO taskDTO) {
        log.info("Updating task: taskId={}", taskId);

        var protoRequest = UpdateTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .setTaskDTO(com.zequent.framework.common.proto.TaskProtoDTO.newBuilder()
                        .setId(taskId)
                        .setName(taskDTO.getName())
                        .setFlightId(taskDTO.getFlightId() != null ? taskDTO.getFlightId() : "")
                        .setSnNumber(taskDTO.getSnNumber())
                        .setMissionId(taskDTO.getMissionId().toString())
                        .build())
                .build();

        return executeAsync(missionAutonomyService.updateTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> getTask(String taskId) {
        log.info("Getting task: taskId={}", taskId);

        var protoRequest = GetTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(missionAutonomyService.getTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> getTaskByFlightId(String flightId) {
        log.info("Getting task by flightId: flightId={}", flightId);

        var protoRequest = GetTaskRequest.newBuilder()
                .setBase(buildBase())
                .setFlightId(flightId)
                .build();

        return executeAsync(missionAutonomyService.getTaskByFlightId(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> deleteTask(String taskId) {
        log.info("Deleting task: taskId={}", taskId);

        var protoRequest = DeleteTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(missionAutonomyService.deleteTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> startTask(String taskId) {
        log.info("Starting task: taskId={}", taskId);

        var protoRequest = StartTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(missionAutonomyService.startTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> stopTask(String taskId) {
        log.info("Stopping task: taskId={}", taskId);

        var protoRequest = StopTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(missionAutonomyService.stopTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> createScheduler(SchedulerDTO schedulerDTO) {
        log.info("Creating scheduler: name={}", schedulerDTO.getName());

        var schedulerBuilder = com.zequent.framework.common.proto.SchedulerProtoDTO.newBuilder()
                .setName(schedulerDTO.getName())
                .setType(schedulerDTO.getType())
                .setActive(schedulerDTO.getActive())
                .setCronExpression(schedulerDTO.getCronExpression() != null ? schedulerDTO.getCronExpression() : "")
                .setClientTimeZone(schedulerDTO.getClientTimeZone() != null ? schedulerDTO.getClientTimeZone() : "");

        if (schedulerDTO.getTaskId() != null) {
            schedulerBuilder.setTaskId(schedulerDTO.getTaskId().toString());
        }
        if (schedulerDTO.getMissionId() != null) {
            schedulerBuilder.setMissionId(schedulerDTO.getMissionId().toString());
        }

        var protoRequest = CreateSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerDTO(schedulerBuilder.build())
                .build();

        return executeAsync(missionAutonomyService.createScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> updateScheduler(String schedulerId, SchedulerDTO schedulerDTO) {
        log.info("Updating scheduler: schedulerId={}", schedulerId);

        var schedulerBuilder = com.zequent.framework.common.proto.SchedulerProtoDTO.newBuilder()
                .setId(schedulerId)
                .setName(schedulerDTO.getName())
                .setType(schedulerDTO.getType())
                .setActive(schedulerDTO.getActive())
                .setCronExpression(schedulerDTO.getCronExpression() != null ? schedulerDTO.getCronExpression() : "")
                .setClientTimeZone(schedulerDTO.getClientTimeZone() != null ? schedulerDTO.getClientTimeZone() : "");

        if (schedulerDTO.getTaskId() != null) {
            schedulerBuilder.setTaskId(schedulerDTO.getTaskId().toString());
        }
        if (schedulerDTO.getMissionId() != null) {
            schedulerBuilder.setMissionId(schedulerDTO.getMissionId().toString());
        }

        var protoRequest = UpdateSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .setSchedulerDTO(schedulerBuilder.build())
                .build();

        return executeAsync(missionAutonomyService.updateScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> getScheduler(String schedulerId) {
        log.info("Getting scheduler: schedulerId={}", schedulerId);

        var protoRequest = GetSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .build();

        return executeAsync(missionAutonomyService.getScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> deleteScheduler(String schedulerId) {
        log.info("Deleting scheduler: schedulerId={}", schedulerId);

        var protoRequest = DeleteSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .build();

        return executeAsync(missionAutonomyService.deleteScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    private RequestBase buildBase() {
        return RequestBase.newBuilder()
                .setTid(UUID.randomUUID().toString())
                .setTimestamp(ProtobufHelpers.now())
                .build();
    }

    private <T> CompletableFuture<T> executeAsync(io.smallrye.mutiny.Uni<T> uni) {
        CompletableFuture<T> future = new CompletableFuture<>();

        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        // Apply resilience (retry + circuit breaker) and timeout
        resilience.executeWithResilience(uni)
                .ifNoItem().after(java.time.Duration.ofSeconds(timeout)).fail()
                .subscribe().with(
                        future::complete,
                        throwable -> {
                            log.error("Mission autonomy request failed: {}", throwable.getMessage(), throwable);
                            future.completeExceptionally(new RuntimeException("Mission autonomy request failed", throwable));
                        }
                );

        return future;
    }

    private MissionResponse toMissionResponse(com.zequent.framework.services.mission.proto.MissionResponse proto) {
        var builder = MissionResponse.builder()
                .success(!proto.getHasErrors())
                .tid(proto.getTid())
                .missionId(proto.getMissionId())
                .timestamp(ProtobufHelpers.toLocalDateTime(proto.getTimestamp()));

        if (proto.hasError()) {
            builder.error(MissionResponse.ErrorInfo.builder()
                    .errorCode(proto.getError().getErrorCode().name())
                    .errorMessage(proto.getError().getErrorMessage())
                    .timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
                    .build());
        }

        if (proto.hasProgress()) {
            builder.progress(MissionResponse.ProgressInfo.builder()
                    .progress(proto.getProgress().getProgress())
                    .state(proto.getProgress().getState())
                    .leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
                    .build());
        }

        if (proto.hasMissionDTO()) {
            var missionData = MissionResponse.MissionData.builder()
                    .missionId(proto.getMissionDTO().hasId() ? proto.getMissionDTO().getId() : null)
                    .name(proto.getMissionDTO().getName())
                    .description(proto.getMissionDTO().getDescription());

            if (proto.getMissionDTO().getAssignedAssetsCount() > 0) {
                missionData.assetSn(proto.getMissionDTO().getAssignedAssets(0));
            }

            builder.missionData(missionData.build());
        }

        return builder.build();
    }

    private TaskResponse toTaskResponse(com.zequent.framework.services.mission.proto.TaskResponse proto) {
        var builder = TaskResponse.builder()
                .success(!proto.getHasErrors())
                .tid(proto.getTid())
                .taskId(proto.getTaskId())
                .timestamp(ProtobufHelpers.toLocalDateTime(proto.getTimestamp()));

        if (proto.hasError()) {
            builder.error(TaskResponse.ErrorInfo.builder()
                    .errorCode(proto.getError().getErrorCode().name())
                    .errorMessage(proto.getError().getErrorMessage())
                    .timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
                    .build());
        }

        if (proto.hasProgress()) {
            builder.progress(TaskResponse.ProgressInfo.builder()
                    .progress(proto.getProgress().getProgress())
                    .state(proto.getProgress().getState())
                    .leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
                    .build());
        }

        if (proto.hasTaskDTO()) {
            builder.taskData(TaskResponse.TaskData.builder()
                    .taskId(proto.getTaskDTO().hasId() ? proto.getTaskDTO().getId() : null)
                    .name(proto.getTaskDTO().hasName() ? proto.getTaskDTO().getName() : null)
                    .flightId(proto.getTaskDTO().hasFlightId() ? proto.getTaskDTO().getFlightId() : null)
                    .assetSn(proto.getTaskDTO().hasSnNumber() ? proto.getTaskDTO().getSnNumber() : null)
                    .build());
        }

        return builder.build();
    }

    private SchedulerResponse toSchedulerResponse(com.zequent.framework.services.mission.proto.SchedulerResponse proto) {
        var builder = SchedulerResponse.builder()
                .success(!proto.getHasErrors())
                .tid(proto.getTid())
                .schedulerId(proto.getSchedulerId())
                .timestamp(ProtobufHelpers.toLocalDateTime(proto.getTimestamp()));

        if (proto.hasError()) {
            builder.error(SchedulerResponse.ErrorInfo.builder()
                    .errorCode(proto.getError().getErrorCode().name())
                    .errorMessage(proto.getError().getErrorMessage())
                    .timestamp(ProtobufHelpers.toLocalDateTime(proto.getError().getTimestamp()))
                    .build());
        }

        if (proto.hasProgress()) {
            builder.progress(SchedulerResponse.ProgressInfo.builder()
                    .progress(proto.getProgress().getProgress())
                    .state(proto.getProgress().getState())
                    .leftTimeInSeconds(proto.getProgress().getLeftTimeInSeconds())
                    .build());
        }

        if (proto.hasSchedulerDTO()) {
            builder.schedulerData(SchedulerResponse.SchedulerData.builder()
                    .schedulerId(proto.getSchedulerDTO().getId())
                    .name(proto.getSchedulerDTO().getName())
                    .startDate(null)
                    .endDate(null)
                    .build());
        }

        return builder.build();
    }
}
