package com.optman.rtsp;

import java.io.IOException;

import com.optman.rtsp.TcpSender;
import com.optman.rtp.sender.RtpSocket;

public class TcpChannel implements RtpSocket{
	int channelNum;
	TcpSender sender;
	public TcpChannel(TcpSender sender, int channelNum){
		this.sender = sender;
		this.channelNum = channelNum;
	}
	@Override
	public void sendPacket(byte[] data, int offset, int size) throws IOException{
		sender.sendData(channelNum, data, offset, size);
	}
	
	public void close(){
		
	}
}