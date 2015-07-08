package com.optman.rtsp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.optman.rtp.sender.RtpSocket;

public class TcpSender{
	
	private Socket socket;
	
	public TcpSender(Socket socket){
		this.socket = socket;
	}
	
	public synchronized void sendData(int channel, byte[] data, int offset, int size) throws IOException{
		DataOutputStream s = new DataOutputStream(socket.getOutputStream());
		
		s.write(36);
		s.write(channel);
		s.writeShort(size);
		s.write(data, offset, size);
	}
	
	public void close(){
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}





