package com.optman.RtspServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.optman.rtp.sender.RtpSocket;

public class RtpSessionManager implements RtpSocket{

	private Set<RtpSession> sessions = new HashSet<RtpSession>();

	private DatagramSocket s;
	
	public RtpSessionManager(){

		try {
			s = new DatagramSocket();
				
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
	}

	public synchronized void add(RtpSession session) {
		sessions.add(session);		
	}

	public synchronized void remove(RtpSession session) {
		sessions.remove(session);		
	}

	
	protected synchronized void sendData(byte[] data, int offset, int size){
		
		Iterator<RtpSession> iter = sessions.iterator();
		
		while(iter.hasNext()){
		
			RtpSession session = iter.next();
			
			boolean hasError = false;
			
			try {
				if(session.tcpSocket != null){
					session.tcpSocket.sendPacket(data, offset, size);
				}else{
					DatagramPacket p;
					p = new DatagramPacket(data, offset, size, session.host, session.port);
					s.send(p);
				}
				
			} catch (UnknownHostException e) {
				hasError = true;
			} catch (IOException e) {
				hasError = true;
			}
			
			if(hasError)
				iter.remove();
		
		}
	}

	@Override
	public void sendPacket(byte[] data, int offset, int size) {
		sendData(data, offset, size);
		
	}
	
	public void close(){
		
	}
	
	protected synchronized String dump(){
		
		String result = "";
		
		Iterator<RtpSession> iter = sessions.iterator();
		
		while(iter.hasNext()){
		
			RtpSession session = iter.next();
			
			result += session.host.toString() + " : " + session.port + "\r\n";			
		}		
		
		return result;
	}

	
	
}
