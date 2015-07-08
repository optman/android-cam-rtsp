package com.optman.rtsp;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import com.optman.rtsp.TcpDataHandler;

public class TcpReceiver{
	private Socket socket;
	
	public TcpReceiver(Socket socket){
		this.socket = socket;
	}
	
	public void run(TcpDataHandler handler) throws IOException{
		
		DataInputStream s = new DataInputStream(socket.getInputStream());
		
		while(true){
		
			if(36 != s.read())
				throw new IOException("invalid magic number");
		
			int channel = s.read();
			int dataSize = s.readShort();
			byte[] data = new byte[dataSize];
			s.readFully(data);
			
			handler.onData(channel, data, dataSize);
		
		}			
		
	}
}
