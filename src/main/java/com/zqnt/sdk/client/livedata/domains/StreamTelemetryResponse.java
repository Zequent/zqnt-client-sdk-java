package com.zqnt.sdk.client.livedata.domains;

import com.zqnt.utils.common.proto.ErrorCode;
import com.zqnt.utils.edge.sdk.domains.AssetTelemetryData;
import com.zqnt.utils.edge.sdk.domains.SubAssetTelemetryData;
import com.zqnt.utils.livedata.proto.TelemetrySourceState;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StreamTelemetryResponse {

	private String tid;
	private Instant timestamp;
	private boolean hasErrors;
	private String sn;
	private String assetId;
	private AssetTelemetryData assetTelemetry;
	private SubAssetTelemetryData subAssetTelemetry;
	private ErrorInfo error;
	private StreamEventType eventType;
	private StreamHeartbeat streamHeartbeat;
	private SourceStatus sourceStatus;

	/** Keeps the constructor from SDK versions before stream control events binary-compatible. */
	public StreamTelemetryResponse(String tid, Instant timestamp, boolean hasErrors, String sn, String assetId,
	                               AssetTelemetryData assetTelemetry, SubAssetTelemetryData subAssetTelemetry,
	                               ErrorInfo error) {
		this(tid, timestamp, hasErrors, sn, assetId, assetTelemetry, subAssetTelemetry, error,
				StreamEventType.UNKNOWN, null, null);
	}

	public enum StreamEventType {
		TELEMETRY,
		HEARTBEAT,
		SOURCE_STATUS,
		ERROR,
		UNKNOWN
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StreamHeartbeat {
		private Instant timestamp;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SourceStatus {
		private String sn;
		private TelemetrySourceState state;
		private Instant observedAt;
		private Instant lastTelemetryAt;
	}


	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ErrorInfo {
		private ErrorCode errorCode;
		private String errorMessage;
		private LocalDateTime timestamp;
	}
}
