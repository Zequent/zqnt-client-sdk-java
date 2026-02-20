package com.zequent.framework.client.sdk.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StreamTelemetryRequest {

	private String sn;
	private String tid;
	private int frequencyMs;
	private int duration;
	private LocalDateTime timestamp;

}
