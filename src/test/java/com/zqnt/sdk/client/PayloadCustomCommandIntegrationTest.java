package com.zqnt.sdk.client;

import com.zqnt.sdk.client.remotecontrol.domains.CapabilityDescriptor;
import com.zqnt.sdk.client.remotecontrol.domains.CapabilityTargetType;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandRequest;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live example for discovering a payload capability and toggling the parachute LED.
 *
 * <p>Discovery only:</p>
 * <pre>
 * mvn -Dtest=PayloadCustomCommandIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dpayload.test.sn=YOUR_DOCK_SN test
 * </pre>
 *
 * <p>Actually switch the LED on and back off (changes hardware state):</p>
 * <pre>
 * mvn -Dtest=PayloadCustomCommandIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dpayload.command.execution.enabled=true \
 *     -Dpayload.test.sn=YOUR_DOCK_SN \
 *     -Dpayload.test.led-on-millis=2000 test
 * </pre>
 */
@Slf4j
@Tag("integration")
class PayloadCustomCommandIntegrationTest {

    private static final String DOCK_SN = System.getProperty(
            "payload.test.sn", "8UUXN2Q00A01FZ");
    private static final String COMMAND = "parachute.led.set";
    private static final boolean EXECUTION_ENABLED = true;
    private static final long LED_ON_MILLIS = Long.getLong(
            "payload.test.led-on-millis", 40_000L);

    private static ZequentClient client;

    @BeforeAll
    static void createClient() {
        client = ZequentClient.builder()
                .remoteControl()
                    .host(System.getProperty("remote.control.test.host", "localhost"))
                    .port(Integer.getInteger("remote.control.test.port", 8002))
                    .usePlaintext(true)
                    .done()
                .maxRetryAttempts(0)
                .requestTimeoutSeconds(10)
                .build();
    }

    @AfterAll
    static void closeClient() {
        if (client != null) client.close();
    }

    @Test
    void discoversParachuteCapabilityAndBuildsRequestWithItsTarget() throws Exception {
        CapabilityDescriptor capability = resolveParachuteLedCapability();

        CustomCommandRequest request = ledRequest(capability, 1);

        assertEquals(DOCK_SN, request.getSn());
        assertEquals(COMMAND, request.getCommandType());
        assertEquals(CapabilityTargetType.PAYLOAD, request.getTargetType());
        assertEquals(capability.getTargetRef(), request.getComponentId());
        assertEquals(Map.of("value", 1), request.getParams());
        log.info("Resolved {} to payload target {}", COMMAND, capability.getTargetRef());
    }

    @Test
    void switchesParachuteLedOnAndBackOffWhenExplicitlyEnabled() throws Exception {
        Assumptions.assumeTrue(EXECUTION_ENABLED,
                "Set -Dpayload.command.execution.enabled=true to execute hardware commands");

        CapabilityDescriptor capability = resolveParachuteLedCapability();
        try {
            assertSuccessful(send(ledRequest(capability, 1)));
            log.info("Parachute LED switched on for {} ms", LED_ON_MILLIS);
            TimeUnit.MILLISECONDS.sleep(LED_ON_MILLIS);
        } finally {
            assertSuccessful(send(ledRequest(capability, 0)));
            log.info("Parachute LED switched off");
        }
    }

    private static CapabilityDescriptor resolveParachuteLedCapability() throws Exception {
        var snapshot = client.remoteControl().getCapabilities(DOCK_SN)
                .get(15, TimeUnit.SECONDS);
        return snapshot.getCapabilities().stream()
                .filter(capability -> COMMAND.equals(capability.getCommandId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(COMMAND + " is not advertised for dock "
                        + DOCK_SN + ". Ensure the PSDK name contains 'flyfire' or 'parachute'."));
    }

    private static CustomCommandRequest ledRequest(CapabilityDescriptor capability, int value) {
        return CustomCommandRequest.forCapability(DOCK_SN, capability, Map.of("value", value));
    }

    private static CustomCommandResponse send(CustomCommandRequest request) throws Exception {
        return client.remoteControl().sendCustomCommand(request).get(15, TimeUnit.SECONDS);
    }

    private static void assertSuccessful(CustomCommandResponse response) {
        assertNotNull(response.getTid());
        assertEquals(COMMAND, response.getCommandType());
        assertTrue(response.isSuccess(), () -> response.getError() != null
                ? response.getError().getErrorCode() + ": " + response.getError().getErrorMessage()
                : response.getMessage());
    }
}
