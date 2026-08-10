package com.zqnt.sdk.client.missionautonomy.capabilities;

/** Idempotent lifecycle command for start, pause, resume, or cancel. */
public record CapabilityLifecycleCommand(String executionId, String reason, String idempotencyKey) {
    public CapabilityLifecycleCommand {
        executionId = CapabilityExecutionCommand.requireText(executionId, "executionId");
    }

    public static CapabilityLifecycleCommand forExecution(String executionId) {
        return new CapabilityLifecycleCommand(executionId, null, null);
    }
}
