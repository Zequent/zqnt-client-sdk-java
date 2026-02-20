package com.zqnt.sdk.client.livedata.domains;

import com.zequent.framework.common.proto.ErrorCodes;
import com.zqnt.utils.edge.sdk.domains.AssetTelemetryData;
import com.zqnt.utils.edge.sdk.domains.SubAssetTelemetryData;
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


	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ErrorInfo {
		private ErrorCodes errorCode;
		private String errorMessage;
		private LocalDateTime timestamp;
	}
}
