package com.zqnt.sdk.client.remotecontrol.domains;

import lombok.Data;

@Data
public class ManualControlInput {

	private String sn;
	private Float roll;
	private Float pitch;
	private Float yaw;
	private Float throttle;
	private Float gimbalPitch;

}
