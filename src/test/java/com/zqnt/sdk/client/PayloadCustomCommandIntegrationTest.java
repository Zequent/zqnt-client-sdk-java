package com.zqnt.sdk.client;

import com.zqnt.sdk.client.connector.domains.AssetPayloadListResponse;
import com.zqnt.sdk.client.connector.domains.ConnectorRequestContext;
import com.zqnt.sdk.client.connector.domains.ListAssetPayloadsRequest;
import com.zqnt.sdk.client.remotecontrol.domains.CustomCommandRequest;
import com.zqnt.utils.JsonUtils;
import com.zqnt.utils.asset.domains.AssetPayloadDTO;
import com.zqnt.utils.asset.domains.PayloadCommandDefinitionDTO;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example integration flow for dynamic payload commands.
 *
 * <p>Discovery and request construction:</p>
 * <pre>
 * mvn -Dtest=PayloadCustomCommandIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dpayload.test.sn=YOUR_ASSET_OR_SUBASSET_SN \
 *     -Dpayload.test.command=searchlight.mode.set test
 * </pre>
 *
 * <p>Actually execute the command (can change hardware state):</p>
 * <pre>
 * mvn -Dtest=PayloadCustomCommandIntegrationTest \
 *     -Dtest.excludedGroups= \
 *     -Dpayload.command.execution.enabled=true \
 *     -Dpayload.test.sn=YOUR_ASSET_OR_SUBASSET_SN \
 *     -Dpayload.test.command=searchlight.mode.set \
 *     -Dpayload.test.params='{"mode":0,"group":0}' test
 * </pre>
 */
@Slf4j
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayloadCustomCommandIntegrationTest {

    private static final String SN = System.getProperty(
            "payload.test.sn", "1581F8HGX261L00A1D0Q");
    private static final String COMMAND = System.getProperty(
            "payload.test.command", "searchlight.mode.set");

    private static ZequentClient client;

    @BeforeAll
    static void createClient() {
        client = ZequentClient.builder()
                .remoteControl()
                    .host(System.getProperty("remote.control.test.host", "localhost"))
                    .port(Integer.getInteger("remote.control.test.port", 8002))
                    .usePlaintext(true)
                    .done()
                .connector()
                    .host(System.getProperty("connector.test.host", "localhost"))
                    .port(Integer.getInteger("connector.test.port", 8010))
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
    @Order(1)
    void fetchesPayloadCommandAndBuildsThePublicClientRequest() throws Exception {
        ResolvedCommand resolved = resolveCommand();

        CustomCommandRequest request = buildClientRequest(resolved);

        assertEquals(SN, request.getSn());
        assertEquals(resolved.componentId(), request.getComponentId());
        assertEquals(COMMAND, request.getCommandType());
        assertFalse(request.getParams().isEmpty());

        log.info("Built payload command request: sn={}, componentId={}, command={}, params={}",
                request.getSn(), request.getComponentId(), request.getCommandType(), request.getParams());
    }

    @Test
    @Order(2)
    void sendsTheResolvedPayloadCommandWhenExplicitlyEnabled() throws Exception {

        ResolvedCommand resolved = resolveCommand();
        CustomCommandRequest request = buildClientRequest(resolved);

        var response = client.remoteControl().sendCustomCommand(request)
                .get(15, TimeUnit.SECONDS);

        assertNotNull(response.getTid());
        assertEquals(COMMAND, response.getCommandType());
        assertTrue(response.isSuccess(), () -> response.getError() != null
                ? response.getError().getErrorCode() + ": " + response.getError().getErrorMessage()
                : response.getMessage());
    }

    private static ResolvedCommand resolveCommand() throws Exception {
        List<AssetPayloadDTO> payloads = fetchPayloads();
        String available = payloads.stream()
                .filter(payload -> payload.getCommands() != null)
                .flatMap(payload -> payload.getCommands().stream())
                .map(PayloadCommandDefinitionDTO::getCommand)
                .distinct().sorted().toList().toString();

        return payloads.stream()
                .filter(payload -> Boolean.TRUE.equals(payload.getActive()))
                .filter(payload -> payload.getCommands() != null)
                .flatMap(payload -> payload.getCommands().stream()
                        .filter(command -> COMMAND.equals(command.getCommand()))
                        .filter(command -> Boolean.TRUE.equals(command.getAvailable()))
                        .map(command -> new ResolvedCommand(payload, command, componentId(payload))))
                .filter(resolved -> resolved.componentId() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No active payload advertises command '" + COMMAND + "'. Available: " + available));
    }

    private static List<AssetPayloadDTO> fetchPayloads() throws Exception {
        ConnectorRequestContext context = ConnectorRequestContext.builder()
                .assetId("c6da1552-4bb2-452e-bf9b-0201744caeaf")
                .clientId("client-sdk-integration-test")
                .build();

        AssetPayloadListResponse response = listPayloads(
                ListAssetPayloadsRequest.builder().context(context).build());
        List<AssetPayloadDTO> payloads = response.getPayloads();

        assertFalse(payloads.isEmpty(), () -> "No registered payloads found for SN " + SN
                + ". The DJI adapter must first receive a PSDK widget state so StateSubscriber "
                + "can register the payload definitions in Connector.");
        return payloads;
    }

    private static CustomCommandRequest buildClientRequest(ResolvedCommand resolved) {
        return CustomCommandRequest.builder()
                .sn(SN)
                .componentId(resolved.componentId())
                .commandType(resolved.command().getCommand())
                .params(commandParams())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> commandParams() {
        String configured = System.getProperty("payload.test.params");
        if (configured != null && !configured.isBlank()) {
            return JsonUtils.fromJson(configured, Map.class);
        }
        return switch (COMMAND) {
            case "searchlight.mode.set" -> Map.of("mode", 0, "group", 0);
            case "searchlight.brightness.set" -> Map.of("brightness", 50, "group", 0);
            case "parachute.led.set" -> Map.of("index", 1, "value", 0);
            case "widget.set" -> Map.of("index", 0, "value", 0);
            default -> Map.of("value", 0);
        };
    }

    private static AssetPayloadListResponse listPayloads(ListAssetPayloadsRequest request) throws Exception {
        final AssetPayloadListResponse response;
        try {
            response = client.connector().listAssetPayloads(request).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception;
            while (cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof StatusRuntimeException grpcError
                    && grpcError.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                throw new AssertionError("Connector endpoint ListAssetPayloads is not implemented. "
                        + "The payloads are registered, but the client cannot discover them until "
                        + "the Connector service implements this RPC.", exception);
            }
            throw exception;
        }

        assertTrue(response.isSuccess(), () -> response.getError() != null
                ? response.getError().getErrorCode() + ": " + response.getError().getErrorMessage()
                : "Connector could not list payloads for SN " + SN);
        return response;
    }

    private static String componentId(AssetPayloadDTO payload) {
        if (payload.getExternalId() != null && !payload.getExternalId().isBlank()) {
            return payload.getExternalId();
        }
        if (payload.getSerialNumber() != null && !payload.getSerialNumber().isBlank()) {
            return payload.getSerialNumber();
        }
        return null;
    }

    private record ResolvedCommand(AssetPayloadDTO payload,
                                   PayloadCommandDefinitionDTO command,
                                   String componentId) { }
}
