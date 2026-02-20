package com.zequent.framework.client.sdk.remotecontrol;

import com.zequent.framework.client.sdk.models.ManualControlInput;
import lombok.extern.slf4j.Slf4j;

/**
 * Example usage of Manual Control Input streaming.
 *
 * This demonstrates how to send multiple control inputs over a gRPC stream.
 */
@Slf4j
public class ManualControlInputExample {

    public static void example(RemoteControl remoteControl, String sn) {
        // Start the manual control input session
        try (ManualControlInputSession session = remoteControl.startManualControlInput(sn, null)) {

            // Send multiple control inputs over the stream
            for (int i = 0; i < 100; i++) {
                ManualControlInput input = new ManualControlInput();
                input.setSn(sn);
                input.setRoll(0.5f);
                input.setPitch(0.3f);
                input.setYaw(0.0f);
                input.setThrottle(0.7f);
                input.setGimbalPitch(-15.0f);

                session.sendInput(input);

                // Optional: Add delay between inputs
                Thread.sleep(50);
            }

            // Complete the stream and get the response
            var response = session.complete();

            if (response.isSuccess()) {
				log.info("Manual control completed successfully: {}", response.getMessage());
            } else {
				log.error("Manual control failed: {}", response.getError());
            }

        } catch (Exception e) {
			log.error("Error during manual control: {}", e.getMessage(), e);
        }
    }

    public static void exampleWithErrorHandling(RemoteControl remoteControl, String sn) {
        ManualControlInputSession session = null;
        try {
            session = remoteControl.startManualControlInput(sn, null);

            // Send control inputs
            ManualControlInput input = new ManualControlInput();
            input.setSn(sn);
            input.setThrottle(0.5f);
            session.sendInput(input);

            // ... more inputs ...

            // Complete normally
            var response = session.complete();
            System.out.println("Success: " + response.getMessage());

        } catch (Exception e) {
            if (session != null) {
                // Complete with error if something goes wrong
                session.completeWithError(e);
            }
            throw new RuntimeException("Manual control failed", e);
        }
    }
}
