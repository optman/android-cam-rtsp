package com.optman.rtp.receiver;

public interface RtpSink {
	public void onRtp(byte[] data, int dataSize);
}
