package com.optman.rtp.player;

import com.optman.rtp.player.Statistics;
import com.google.android.exoplayer.MediaFormat;

public interface Player {
	public void start();
	public void stop();
	public void setVideoFormat(MediaFormat format);
	public void setAudioFormat(MediaFormat format);
	public void addVideoPacket(byte[] data, int dataSize);
	public void addAudioPacket(byte[] data, int dataSize);
	public Statistics getStats();
	public void setJitterBuffer(long timeUs);
}
