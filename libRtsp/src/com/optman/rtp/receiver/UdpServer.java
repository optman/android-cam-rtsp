package com.optman.rtp.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.optman.rtp.receiver.RtpSink;

import android.util.Log;

public class UdpServer implements Runnable{

	private DatagramSocket s;
	private int port;
	protected RtpSink sink;
	private Thread thread;
	
	public UdpServer(int port, RtpSink sink){
		this.port = port;
		this.sink = sink;
	}
	
	public void open() throws SocketException{
		Log.i("RTP", "listen at " + port);
		s = new DatagramSocket(port);
		
		thread = new Thread(this);
		thread.start();
	}
	
	public void close(){
		s.close();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		byte[] buf = new byte[1500];
		
		while(true){
			try {
				DatagramPacket pack = new DatagramPacket(buf, buf.length);
				s.receive(pack);

				if(sink != null)
					sink.onRtp(pack.getData(), pack.getLength());
				
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
}
