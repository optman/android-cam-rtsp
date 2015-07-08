package com.optman.rtp.sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.optman.rtp.sender.RtpSocket;

public class RtpUdp implements RtpSocket {

	private DatagramSocket s;

	private InetAddress addr;
	private int port;

	
	public RtpUdp(String host, int port, boolean broadcast){

		try {
			this.addr = InetAddress.getByName(host);
			this.port = port;

			s = new DatagramSocket();
			s.setBroadcast(broadcast);
				
		} catch (SocketException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}
	
	public void close(){
		s.close();
	}
	
	@Override
	public void sendPacket(byte[] data, int offset, int size) {
		try {
			DatagramPacket p;
			p = new DatagramPacket(data, offset, size, addr, port);
			
			/*
			long nowMs =  System.nanoTime()/1000000;
			
			long deltaMs = nowMs - lastSentTimeMs;
			
			if(deltaMs < 1)
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
			}
			
			lastSentTimeMs = nowMs;
			*/
			
			s.send(p);
			
			
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
