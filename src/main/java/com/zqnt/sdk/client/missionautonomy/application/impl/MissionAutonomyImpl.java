package com.zqnt.sdk.client.missionautonomy.application.impl;

import com.zequent.framework.common.proto.*;
import com.zqnt.sdk.client.config.GrpcClientConfig;
import com.zqnt.sdk.client.grpc.GrpcResilience;
import com.zqnt.sdk.client.missionautonomy.application.MissionAutonomy;
import com.zqnt.sdk.client.missionautonomy.domains.MissionResponse;
import com.zqnt.sdk.client.missionautonomy.domains.SchedulerResponse;
import com.zqnt.sdk.client.remotecontrol.domains.TaskResponse;
import com.zequent.framework.services.mission.proto.*;
import com.zqnt.utils.core.ProtobufHelpers;
import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import com.zqnt.utils.missionautonomy.domains.WaypointDTO;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Mission Autonomy client implementation using standard gRPC stubs.
 * Performance optimizations:
 * - Uses FutureStub for optimal async unary calls
 * - CompletableFuture for framework-agnostic async operations
 * - Built-in resilience with retry and circuit breaker
 * - Dedicated executor for callback handling
 */
@Slf4j
public class MissionAutonomyImpl implements MissionAutonomy {

    private final MissionAutonomyServiceGrpc.MissionAutonomyServiceFutureStub futureStub;
    private final GrpcResilience resilience;
    private final GrpcClientConfig config;
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService timeoutScheduler;

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
        this.futureStub = MissionAutonomyServiceGrpc.newFutureStub(channel);

        // Dedicated executor for gRPC callbacks
        this.callbackExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "mission-autonomy-callback");
            t.setDaemon(true);
            return t;
        });

        // Scheduler for timeout handling (shared)
        this.timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "mission-autonomy-timeout");
            t.setDaemon(true);
            return t;
        });

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

        var missionBuilder = mapMissionDtoToProto(MissionProtoDTO.newBuilder(), missionDTO);

        var protoRequest = CreateMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionDTO(missionBuilder.build())
                .build();

        return executeAsync(() -> futureStub.createMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<MissionResponse> updateMission(String missionId, MissionDTO missionDTO) {
        log.info("Updating mission: missionId={}", missionId);

        var missionBuilder = mapMissionDtoToProto(MissionProtoDTO.newBuilder()
                .setId(missionId), missionDTO);

        var protoRequest = UpdateMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .setMissionDTO(missionBuilder.build())
                .build();

        return executeAsync(() -> futureStub.updateMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    private static MissionProtoDTO.@NonNull Builder mapMissionDtoToProto(MissionProtoDTO.Builder missionId, MissionDTO missionDTO) {
        var missionBuilder = missionId
                .setName(missionDTO.getName() != null ? missionDTO.getName() : "")
                .setDescription(missionDTO.getDescription() != null ? missionDTO.getDescription() : "");

        if (missionDTO.getStatus() != null) {
            missionBuilder.setStatus(missionDTO.getStatus());
        }
        if (missionDTO.getType() != null) {
            missionBuilder.setType(missionDTO.getType());
        }
        if (missionDTO.getGeoJson() != null) {
            missionBuilder.setGeoJson(missionDTO.getGeoJson());
        }
        if (missionDTO.getStartDate() != null) {
            missionBuilder.setStartDate(ProtobufHelpers.toTimestamp(missionDTO.getStartDate()));
        }
        if (missionDTO.getEndDate() != null) {
            missionBuilder.setEndDate(ProtobufHelpers.toTimestamp(missionDTO.getEndDate()));
        }
        if (missionDTO.getAssignedAssets() != null && !missionDTO.getAssignedAssets().isEmpty()) {
            missionBuilder.addAllAssignedAssets(missionDTO.getAssignedAssets());
        }
        if (missionDTO.getCreatedAt() != null) {
            missionBuilder.setCreatedAt(ProtobufHelpers.toTimestamp(missionDTO.getCreatedAt()));
        }
        if (missionDTO.getModifiedAt() != null) {
            missionBuilder.setUpdatedAt(ProtobufHelpers.toTimestamp(missionDTO.getModifiedAt()));
        }
        return missionBuilder;
    }

    @Override
    public CompletableFuture<MissionResponse> getMission(String missionId) {
        log.info("Getting mission: missionId={}", missionId);

        var protoRequest = GetMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .build();

        return executeAsync(() -> futureStub.getMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<MissionResponse> deleteMission(String missionId) {
        log.info("Deleting mission: missionId={}", missionId);

        var protoRequest = DeleteMissionRequest.newBuilder()
                .setBase(buildBase())
                .setMissionId(missionId)
                .build();

        return executeAsync(() -> futureStub.deleteMission(protoRequest))
                .thenApply(this::toMissionResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> createTask(TaskDTO taskDTO) {
        log.info("Creating task: name={}", taskDTO.getName());

        var taskProtoBuilder = mapTaskDtoToProto(TaskProtoDTO.newBuilder(), taskDTO);

        var protoRequest = CreateTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskDTO(taskProtoBuilder.build())
                .build();

        return executeAsync(() -> futureStub.createTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> updateTask(String taskId, TaskDTO taskDTO) {
        log.info("Updating task: taskId={}", taskId);

        var taskProtoBuilder = mapTaskDtoToProto(TaskProtoDTO.newBuilder()
                .setId(taskId), taskDTO);

        var protoRequest = UpdateTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .setTaskDTO(taskProtoBuilder.build())
                .build();

        return executeAsync(() -> futureStub.updateTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    private static TaskProtoDTO.@NonNull Builder mapTaskDtoToProto(TaskProtoDTO.Builder taskId, TaskDTO taskDTO) {
        var taskProtoBuilder = taskId
                .setName(taskDTO.getName() != null ? taskDTO.getName() : "")
                .setFlightId(taskDTO.getFlightId() != null ? taskDTO.getFlightId() : "")
                .setSnNumber(taskDTO.getSnNumber() != null ? taskDTO.getSnNumber() : "")
                .setAssetId(taskDTO.getAssetId() != null ? taskDTO.getAssetId() : "")
                .setDescription(taskDTO.getDescription() != null ? taskDTO.getDescription() : "")
                .setPayloadImagingType(taskDTO.getPayloadImagingType() != null ? taskDTO.getPayloadImagingType() : "")
                .setFileUrl(taskDTO.getFileUrl() != null ? taskDTO.getFileUrl() : "")
                .setFileMd5(taskDTO.getFileMd5() != null ? taskDTO.getFileMd5() : "")
                .setFlightAreaFileUrl(taskDTO.getFlightAreaFileUrl() != null ? taskDTO.getFlightAreaFileUrl() : "")
                .setFlightAreaChecksum(taskDTO.getFlightAreaChecksum() != null ? taskDTO.getFlightAreaChecksum() : "")
                .setCurrentStep(taskDTO.getCurrentStep() != null ? taskDTO.getCurrentStep() : "");

        if (taskDTO.getMissionId() != null) {
            taskProtoBuilder.setMissionId(taskDTO.getMissionId().toString());
        }
        if (taskDTO.getStatus() != null) {
            taskProtoBuilder.setStatus(taskDTO.getStatus());
        }
        if (taskDTO.getCreatedAt() != null) {
            taskProtoBuilder.setCreatedAt(ProtobufHelpers.toTimestamp(taskDTO.getCreatedAt()));
        }
        if (taskDTO.getModifiedAt() != null) {
            taskProtoBuilder.setUpdatedAt(ProtobufHelpers.toTimestamp(taskDTO.getModifiedAt()));
        }
        if (taskDTO.getFlyToWaylineMode() != null) {
            taskProtoBuilder.setFlyToWaylineMode(taskDTO.getFlyToWaylineMode());
        }
        if (taskDTO.getWaylineFinishAction() != null) {
            taskProtoBuilder.setWaylineFinishAction(taskDTO.getWaylineFinishAction());
        }
        if (taskDTO.getExitWaylineWhenRcLostEnum() != null) {
            taskProtoBuilder.setExitWaylineWhenRcLostEnum(taskDTO.getExitWaylineWhenRcLostEnum());
        }
        if (taskDTO.getRcLostActionEnum() != null) {
            taskProtoBuilder.setRcLostActionEnum(taskDTO.getRcLostActionEnum());
        }
        if (taskDTO.getTakeOffSecurityHeight() != null) {
            taskProtoBuilder.setTakeOffSecurityHeight(taskDTO.getTakeOffSecurityHeight());
        }
        if (taskDTO.getGlobalTransitionSpeed() != null) {
            taskProtoBuilder.setGlobalTransitionSpeed(taskDTO.getGlobalTransitionSpeed());
        }
        if (taskDTO.getWaylineType() != null) {
            taskProtoBuilder.setWaylineType(taskDTO.getWaylineType());
        }
        if (taskDTO.getWaylineTurnMode() != null) {
            taskProtoBuilder.setWaylineTurnMode(taskDTO.getWaylineTurnMode());
        }
        if (taskDTO.getUseStraightLine() != null) {
            taskProtoBuilder.setUseStraightLine(taskDTO.getUseStraightLine());
        }
        if (taskDTO.getGimbalPitchMode() != null) {
            taskProtoBuilder.setGimbalPitchMode(taskDTO.getGimbalPitchMode());
        }
        if (taskDTO.getGlobalGimbalPitch() != null) {
            taskProtoBuilder.setGlobalGimbalPitch(taskDTO.getGlobalGimbalPitch());
        }
        if (taskDTO.getGlobalSpeed() != null) {
            taskProtoBuilder.setGlobalSpeed(taskDTO.getGlobalSpeed());
        }
        if (taskDTO.getGlobalHeight() != null) {
            taskProtoBuilder.setGlobalHeight(taskDTO.getGlobalHeight());
        }
        if (taskDTO.getRthAltitude() != null) {
            taskProtoBuilder.setRthAltitude(taskDTO.getRthAltitude());
        }
        if (taskDTO.getRthMode() != null) {
            taskProtoBuilder.setRthMode(taskDTO.getRthMode());
        }
        if (taskDTO.getRthSpeed() != null) {
            taskProtoBuilder.setRthSpeed(taskDTO.getRthSpeed());
        }
        if (taskDTO.getOutOfControlAction() != null) {
            taskProtoBuilder.setOutOfControlAction(taskDTO.getOutOfControlAction());
        }
        if (taskDTO.getWaylinePrecisionType() != null) {
            taskProtoBuilder.setWaylinePrecisionType(taskDTO.getWaylinePrecisionType());
        }
        if (taskDTO.getCurrentProgress() != null) {
            taskProtoBuilder.setCurrentProgress(taskDTO.getCurrentProgress());
        }
        if (taskDTO.getBreakReason() != null) {
            taskProtoBuilder.setBreakReason(taskDTO.getBreakReason());
        }

        if (!taskDTO.getWaypoints().isEmpty()) {
            taskProtoBuilder.addAllWaypoints(mapWaypointsDtoToProto(taskDTO.getWaypoints()));
        }
        return taskProtoBuilder;
    }

    private static List<WaypointProtoDTO> mapWaypointsDtoToProto(List<WaypointDTO> waypointDTOS) {
        List<WaypointProtoDTO> waypointProtoDTOS = new ArrayList<>();
        waypointDTOS.forEach(waypointDTO -> {
            waypointProtoDTOS.add(WaypointProtoDTO.newBuilder()
                    .setLatitude(waypointDTO.getLatitude())
                    .setLongitude(waypointDTO.getLongitude())
                    .setAltitude(waypointDTO.getAltitude())
                    .setSpeed(waypointDTO.getSpeed())
                    .setWpOrder(waypointDTO.getWpOrder())
                    .setVehicleAction(waypointDTO.getVehicleAction())
                    .setGimbalPitch(waypointDTO.getGimbalPitch())
                    .setFlyTrough(waypointDTO.getFlyThrough())
                    .setTaskId(String.valueOf(waypointDTO.getTask()))
                    .build());
        });
        return waypointProtoDTOS;
    }

    @Override
    public CompletableFuture<TaskResponse> getTask(String taskId) {
        log.info("Getting task: taskId={}", taskId);

        var protoRequest = GetTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(() -> futureStub.getTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> getTaskByFlightId(String flightId) {
        log.info("Getting task by flightId: flightId={}", flightId);

        var protoRequest = GetTaskRequest.newBuilder()
                .setBase(buildBase())
                .setFlightId(flightId)
                .build();

        return executeAsync(() -> futureStub.getTaskByFlightId(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> deleteTask(String taskId) {
        log.info("Deleting task: taskId={}", taskId);

        var protoRequest = DeleteTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(() -> futureStub.deleteTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> startTask(String taskId) {
        log.info("Starting task: taskId={}", taskId);

        var protoRequest = StartTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(() -> futureStub.startTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<TaskResponse> stopTask(String taskId) {
        log.info("Stopping task: taskId={}", taskId);

        var protoRequest = StopTaskRequest.newBuilder()
                .setBase(buildBase())
                .setTaskId(taskId)
                .build();

        return executeAsync(() -> futureStub.stopTask(protoRequest))
                .thenApply(this::toTaskResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> createScheduler(SchedulerDTO schedulerDTO) {
        log.info("Creating scheduler: name={}", schedulerDTO.getName());

        var schedulerBuilder = mapSchedulerDtoToProto(SchedulerProtoDTO.newBuilder(), schedulerDTO);


        var protoRequest = CreateSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerDTO(schedulerBuilder.build())
                .build();

        return executeAsync(() -> futureStub.createScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    private static SchedulerProtoDTO.@NonNull Builder mapSchedulerDtoToProto(SchedulerProtoDTO.Builder newBuilder, SchedulerDTO schedulerDTO) {
        var schedulerBuilder = newBuilder
                .setName(schedulerDTO.getName() != null ? schedulerDTO.getName() : "")
                .setCronExpression(schedulerDTO.getCronExpression() != null ? schedulerDTO.getCronExpression() : "")
                .setClientTimeZone(schedulerDTO.getClientTimeZone() != null ? schedulerDTO.getClientTimeZone() : "");

        if (schedulerDTO.getType() != null) {
            schedulerBuilder.setType(schedulerDTO.getType());
        }
        if (schedulerDTO.getActive() != null) {
            schedulerBuilder.setActive(schedulerDTO.getActive());
        }
        if (schedulerDTO.getTaskId() != null) {
            schedulerBuilder.setTaskId(schedulerDTO.getTaskId().toString());
        }
        if (schedulerDTO.getMissionId() != null) {
            schedulerBuilder.setMissionId(schedulerDTO.getMissionId().toString());
        }
        return schedulerBuilder;
    }

    @Override
    public CompletableFuture<SchedulerResponse> updateScheduler(String schedulerId, SchedulerDTO schedulerDTO) {
        log.info("Updating scheduler: schedulerId={}", schedulerId);

        var schedulerBuilder = mapSchedulerDtoToProto(SchedulerProtoDTO.newBuilder()
                .setId(schedulerId), schedulerDTO);

        var protoRequest = UpdateSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .setSchedulerDTO(schedulerBuilder.build())
                .build();

        return executeAsync(() -> futureStub.updateScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> getScheduler(String schedulerId) {
        log.info("Getting scheduler: schedulerId={}", schedulerId);

        var protoRequest = GetSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .build();

        return executeAsync(() -> futureStub.getScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    @Override
    public CompletableFuture<SchedulerResponse> deleteScheduler(String schedulerId) {
        log.info("Deleting scheduler: schedulerId={}", schedulerId);

        var protoRequest = DeleteSchedulerRequest.newBuilder()
                .setBase(buildBase())
                .setSchedulerId(schedulerId)
                .build();

        return executeAsync(() -> futureStub.deleteScheduler(protoRequest))
                .thenApply(this::toSchedulerResponse);
    }

    private RequestBase buildBase() {
        return RequestBase.newBuilder()
                .setTid(UUID.randomUUID().toString())
                .setTimestamp(ProtobufHelpers.now())
                .build();
    }

    /**
     * Execute async gRPC call with resilience and timeout.
     * Converts ListenableFuture to CompletableFuture with proper resource management.
     */
    private <T> CompletableFuture<T> executeAsync(java.util.function.Supplier<com.google.common.util.concurrent.ListenableFuture<T>> futureSupplier) {
        int timeout = config != null ? config.getRequestTimeoutSeconds() : 30;

        return resilience.executeWithResilienceAsync(() -> {
            CompletableFuture<T> future = new CompletableFuture<>();

            ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
                future.completeExceptionally(new TimeoutException("Request timed out after " + timeout + "s"));
            }, timeout, TimeUnit.SECONDS);

            // Convert ListenableFuture to CompletableFuture
            com.google.common.util.concurrent.Futures.addCallback(
                    futureSupplier.get(),
                    new com.google.common.util.concurrent.FutureCallback<T>() {
                        @Override
                        public void onSuccess(T result) {
                            timeoutTask.cancel(false);
                            future.complete(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            timeoutTask.cancel(false);
                            future.completeExceptionally(t);
                        }
                    },
                    callbackExecutor
            );

            return future;
        });
    }

    /**
     * Shutdown executors when done.
     * Should be called when closing the client.
     */
    public void shutdown() {
        callbackExecutor.shutdown();
        timeoutScheduler.shutdown();
        try {
            if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                callbackExecutor.shutdownNow();
            }
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            callbackExecutor.shutdownNow();
            timeoutScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
