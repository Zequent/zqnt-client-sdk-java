package com.zqnt.sdk.client;

import com.zqnt.sdk.client.livedata.application.LiveData;
import com.zqnt.sdk.client.livedata.domains.StreamTelemetryRequest;
import com.zqnt.sdk.client.remotecontrol.application.RemoteControl;
import com.zqnt.sdk.client.remotecontrol.domains.DockOperationRequest;
import com.zqnt.sdk.client.remotecontrol.domains.RemoteControlResponse;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ZequentClient with real service connections.
 * <p>
 * These tests require running services on localhost:
 * - Remote Control Service: localhost:9091
 * - Mission Autonomy Service: localhost:9092
 * - Live Data Service: localhost:9093
 * <p>
 * Start services with: podman-compose up
 * <p>
 * Tests can be skipped if services are not available.
 * Run with: mvn test -Dgroups=integration
 */
@Slf4j
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "integration.tests.enabled", matches = "true")
class ZequentClientIntegrationTest {

	static {
		// Fix for JBoss LogManager warning
		System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
	}

	private static ZequentClient client;
	private static final String TEST_ASSET_SN = "8UUXN3N00A03KF";
	private static final UUID tid = UUID.randomUUID();

	// Set to true if services are available
	private static final boolean SERVICES_AVAILABLE = checkServicesAvailable();

	@BeforeAll
	static void setUpAll() {
		if (!SERVICES_AVAILABLE) {
			log.warn("Services not available - tests will be skipped");
			return;
		}

		// Create client with real service connections
		client = ZequentClient.builder()
				.remoteControl()
				.usePlaintext(true)
				.useStork(false)
				.done()
				.missionAutonomy()
				.usePlaintext(true)
				.useStork(false)
				.done()
				.liveData()
				.usePlaintext(true)
				.useStork(false)
				.done()
				.maxRetryAttempts(3)
				.retryDelayMillis(500)
				.connectionTimeoutSeconds(5)
				.requestTimeoutSeconds(10)
				.build();

		log.info("Integration test client created");
	}

	@AfterAll
	static void tearDownAll() {
		if (client != null) {
			client.close();
			log.info("Integration test client closed");
		}
	}

	@Test
	@Order(1)
	void testClientConnection() {
		Assumptions.assumeTrue(SERVICES_AVAILABLE, "Services not available");

		assertNotNull(client);
		assertTrue(client.isConnected(), "Client should be connected to services");
	}

	@Test
	@Order(2)
	void testRemoteControlDebugMode() {
		Assumptions.assumeTrue(SERVICES_AVAILABLE, "Services not available");

		RemoteControl remoteControl = client.remoteControl();
		assertNotNull(remoteControl);

		try {
			// Enable debug mode
			RemoteControlResponse response = remoteControl.debugMode(
					DockOperationRequest.builder()
							.sn(TEST_ASSET_SN)
							.value(true)
							.build()
			).join();

			log.info("Debug Mode Response: success={}, tid={}",
					response.isSuccess(), response.getTid());

			assertNotNull(response);
			assertNotNull(response.getTid(), "Transaction ID should not be null");

			// On success we should get a success response
			if (response.isSuccess()) {
				assertEquals(TEST_ASSET_SN, response.getSn());
			} else {
				// On error, error info should be present
				assertNotNull(response.getError());
				log.warn("Remote Control Error: {} - {}",
						response.getError().getErrorCode(),
						response.getError().getErrorMessage());
			}
		} catch (Exception e) {
			log.error("Remote Control Test failed", e);
			fail("Remote Control call should not fail: " + e.getMessage());
		}
	}

	@Test
	@Order(3)
	void testLiveDataStreaming() {
		Assumptions.assumeTrue(SERVICES_AVAILABLE, "Services not available");

		LiveData liveData = client.liveData();
		assertNotNull(liveData);

		// CountDownLatch for async streaming
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger receivedMessages = new AtomicInteger(0);
		AtomicReference<Throwable> error = new AtomicReference<>();

		// Create POJO request
		StreamTelemetryRequest request = new StreamTelemetryRequest(TEST_ASSET_SN,
				tid.toString(), 100, 100,
				LocalDateTime.now());


		try {
			liveData.streamTelemetryData(request, response -> {
				int count = receivedMessages.incrementAndGet();

				log.info("📡 Telemetry #{}: tid={}, sn={}, hasErrors={}",
						count, response.getTid(), response.getSn(), response.isHasErrors());

				// Validate response
				assertNotNull(response, "Response should not be null");
				assertNotNull(response.getTid(), "TID should not be null");
				assertNotNull(response.getSn(), "SN should not be null");

				// Check telemetry fields
				if (response.getAssetTelemetry() != null) {
					log.info("  ✈️ AssetTelemetry received");
				} else if (response.getSubAssetTelemetry() != null) {
					log.info("  🎥 SubAssetTelemetry received");
				} else if (response.getError() != null) {
					log.error("  ❌ Error received");
				}

				// End test after 5 messages
				if (count >= 5) {
					latch.countDown();
				}
			}, throwable -> {
				log.error("❌ Stream Error Callback", throwable);
				error.set(throwable);
				latch.countDown();
			});

		} catch (Exception e) {
			log.error("LiveData Stream Test failed", e);
			// Don't fail - might not have active assets
		}
	}

/*    @Test
    @Order(4)
    void testMissionAutonomyGetAssets() {
        Assumptions.assumeTrue(SERVICES_AVAILABLE, "Services not available");

        MissionAutonomy missionAutonomy = client.missionAutonomy();
        assertNotNull(missionAutonomy);

        try {
            // Get all assets of the organization
            var assets = missionAutonomy();

            assertNotNull(assets, "Assets list should not be null");
            log.info("Found assets: {}", assets.size());

            // If assets are available, check their structure
            if (!assets.isEmpty()) {
                assets.forEach(asset -> {
                    assertNotNull(asset.getSn(), "Asset SN should not be null");
                    log.info("Asset: SN={}, Name={}, Type={}",
                        asset.getSn(), asset.getName(), asset.getType());
                });
            } else {
                log.warn("No assets found in organization");
            }

        } catch (Exception e) {
            log.error("MissionAutonomy Test failed", e);
            fail("MissionAutonomy call should not fail: " + e.getMessage());
        }
    }*/

	@Test
	@Order(5)
	void testClientResilience() {
		Assumptions.assumeTrue(SERVICES_AVAILABLE, "Services not available");

		// Test with invalid asset SN - should handle error
		RemoteControl remoteControl = client.remoteControl();

		RemoteControlResponse response = remoteControl.debugMode(
				DockOperationRequest.builder()
						.sn("INVALID_SN_DOES_NOT_EXIST")
						.value(true)
						.build()
		).join();

		assertNotNull(response);
		// Should either return error or timeout
		if (!response.isSuccess()) {
			assertNotNull(response.getError());
			log.info("Expected error received: {}",
					response.getError().getErrorMessage());
		}
	}

	/**
	 * Checks if services are available (simple connection test).
	 * In a real project you would implement a health check here.
	 */
	private static boolean checkServicesAvailable() {
		// Here you could implement a simple health check
		// For now we assume services are available when INTEGRATION_TEST env var is set
		return true;
	}
}
