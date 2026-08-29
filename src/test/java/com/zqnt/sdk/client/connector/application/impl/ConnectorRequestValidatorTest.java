package com.zqnt.sdk.client.connector.application.impl;

import com.zqnt.sdk.client.connector.domains.ConnectorRequestContext;
import com.zqnt.sdk.client.connector.domains.GetAssetBySnRequest;
import com.zqnt.sdk.client.connector.domains.StoreNotificationRequest;
import com.zqnt.sdk.client.connector.domains.StoreTelemetryRequest;
import com.zqnt.utils.edge.sdk.domains.TelemetryData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorRequestValidatorTest {

    @Test
    void requiresSnForSerialNumberOperations() {
        var request = GetAssetBySnRequest.builder()
                .context(ConnectorRequestContext.builder().sn(" ").build())
                .build();

        var error = assertThrows(IllegalArgumentException.class,
                () -> ConnectorRequestValidator.validate(request));

        assertEquals("Invalid Connector request: context.sn must not be null or blank", error.getMessage());
    }

    @Test
    void rejectsTelemetryWithoutSourceTypeInsteadOfTreatingItAsSubAsset() {
        var request = StoreTelemetryRequest.builder()
                .assetId("asset-1")
                .telemetry(TelemetryData.builder().timestamp(LocalDateTime.now()).build())
                .build();

        var error = assertThrows(IllegalArgumentException.class,
                () -> ConnectorRequestValidator.validate(request));

        assertTrue(error.getMessage().contains("sourceType"));
    }

    @Test
    void rejectsMismatchingOrMultipleNotificationEvents() {
        var mismatching = StoreNotificationRequest.builder()
                .eventType(StoreNotificationRequest.EventType.TASK)
                .severity(StoreNotificationRequest.Severity.INFO)
                .assetStatus(StoreNotificationRequest.AssetStatusEvent.builder().sn("asset-sn").build())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConnectorRequestValidator.validate(mismatching));

        var multiple = StoreNotificationRequest.builder()
                .eventType(StoreNotificationRequest.EventType.ASSET_STATUS)
                .severity(StoreNotificationRequest.Severity.INFO)
                .assetStatus(StoreNotificationRequest.AssetStatusEvent.builder().sn("asset-sn").build())
                .task(StoreNotificationRequest.TaskEvent.builder()
                        .taskId("task-1")
                        .taskType(StoreNotificationRequest.TaskType.TASK_TYPE_DETECT)
                        .status(StoreNotificationRequest.TaskStatus.TASK_RUNNING)
                        .build())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> ConnectorRequestValidator.validate(multiple));
    }

    @Test
    void acceptsTypeSafeTaskNotification() {
        var request = StoreNotificationRequest.builder()
                .eventType(StoreNotificationRequest.EventType.TASK)
                .severity(StoreNotificationRequest.Severity.WARN)
                .task(StoreNotificationRequest.TaskEvent.builder()
                        .taskId("task-1")
                        .taskType(StoreNotificationRequest.TaskType.TASK_TYPE_DETECT)
                        .status(StoreNotificationRequest.TaskStatus.TASK_RUNNING)
                        .build())
                .build();

        assertDoesNotThrow(() -> ConnectorRequestValidator.validate(request));
    }
}
