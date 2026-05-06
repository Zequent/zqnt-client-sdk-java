package com.zqnt.sdk.client.missionautonomy.application;

import com.zqnt.utils.missionautonomy.domains.MissionDTO;
import com.zqnt.utils.missionautonomy.domains.SchedulerDTO;
import com.zqnt.utils.missionautonomy.domains.TaskDTO;
import com.zqnt.sdk.client.missionautonomy.domains.MissionResponse;
import com.zqnt.sdk.client.missionautonomy.domains.SchedulerResponse;
import com.zqnt.sdk.client.missionautonomy.domains.TaskResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MissionAutonomy {

    // Mission operations
    CompletableFuture<MissionResponse> createMission(MissionDTO missionDTO);
    CompletableFuture<MissionResponse> updateMission(String missionId, MissionDTO missionDTO);
    CompletableFuture<MissionResponse> getMission(String missionId);
    CompletableFuture<MissionResponse> deleteMission(String missionId);

    // Task
    CompletableFuture<TaskResponse> createTask(TaskDTO taskDTO);
    CompletableFuture<TaskResponse> updateTask(String taskId, TaskDTO taskDTO);
    CompletableFuture<TaskResponse> getTask(String taskId);
    CompletableFuture<TaskResponse> getTaskByFlightId(String flightId);
    CompletableFuture<TaskResponse> deleteTask(String taskId);
    CompletableFuture<TaskResponse> startTask(String taskId);
    CompletableFuture<TaskResponse> stopTask(String taskId);

    // Schedule
    CompletableFuture<SchedulerResponse> createScheduler(SchedulerDTO request);
    CompletableFuture<SchedulerResponse> updateScheduler(String schedulerId, SchedulerDTO schedulerDTO);
    CompletableFuture<SchedulerResponse> getScheduler(String schedulerId);
    CompletableFuture<SchedulerResponse> deleteScheduler(String schedulerId);
    CompletableFuture<SchedulerResponse> createSchedulers(List<SchedulerDTO> schedulerDTOS);
    CompletableFuture<SchedulerResponse> deleteSchedulers(List<String> schedulerIds);
    CompletableFuture<SchedulerResponse> deleteAllSchedulersByTaskId(String taskId);
    //TODO add batchdelete endpoint here -> up to the core layer handling in db layer mutli delete for schedulers
}
