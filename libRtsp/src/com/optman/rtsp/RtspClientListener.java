package com.optman.rtsp;

import com.optman.rtsp.RtspClient;

public interface RtspClientListener {
	public void onReady(RtspClient client);
	public void onRtpPacket(byte[] data, int dataSize);
}
