package com.optman.rtsp;

public interface TcpDataHandler{
	public void onData(int channel, byte[] data, int dataSize); 
};