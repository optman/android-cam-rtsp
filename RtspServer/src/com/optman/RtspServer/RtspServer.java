package com.optman.RtspServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.optman.rtp.sender.RtpSocket;
import com.optman.rtsp.TcpChannel;
import com.optman.rtsp.TcpSender;
import com.optman.rtsp.*;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

public class RtspServer {

	private ServerSocket server;
	private Thread thread;
	private RtpSessionManager videoSessions;
	private RtpSessionManager audioSessions;
	private final int videoPayloadType = 125;  
	private final int audioPayloadType = 97;  
	private final int audioSampleRate = 44100;
	private final int audioChannel = 2;
	private final String audioConfig = "1210";
	private final int port;
	private StreamInfo stream;
	
	enum MethodName{
		NA,
		Options,
		Describe,
		Setup,
		Play,
		GetParameter,
		TearDown,		
	};
	
	class RequestInfo{
		InetAddress host;
		boolean isTcp;
		int videoPort;
		int audioPort;
		int videoChannel;
		int audioChannel;
		RtpSession videoSession;
		RtpSession audioSession;
		Socket socket;
	};


	public RtspServer(int port, RtpSessionManager videoSessions, RtpSessionManager audioSessions, StreamInfo stream){
	
		this.port = port;
		this.videoSessions = videoSessions;
		this.audioSessions = audioSessions;
		this.stream = stream;
	
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start(){
		thread = new Thread(new Runnable(){

			@Override
			public void run() {
				
				while(true){
					try {
						final Socket client = server.accept();						
						
						(new Thread(new Runnable(){
							public void run() {
								handleClient(client);								
							}})				
						).start();
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
				
				Log.i("RtspServer:", "exit accept.");
			}
			
		});
		
		thread.start();
	}
	
	public void stop(){
		try {
			server.close();
			thread.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handleClient(Socket client){
		
		RequestInfo info = new RequestInfo();
		info.host = client.getInetAddress();
		info.socket = client;
		
		try {
			DataInputStream reader = new DataInputStream(client.getInputStream());
			PrintWriter writer = new PrintWriter(client.getOutputStream());
	
			while(true){
				if(!handleRequest(reader, writer, info)){
					Log.i("RtspServer", "handle request fail.");
					break;
				}
			}
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		removeSession(info);
		
		Log.i("RtspServer", "handle client finish.");
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale") 
	private boolean handleRequest(DataInputStream reader, PrintWriter writer, RequestInfo info) throws IOException{
    	
		MethodName method = MethodName.NA;
		Map<String, String> headers = new HashMap<String, String>();
		
		String line = reader.readLine();
		if(line == null){
			Log.i("RtspServer", "line is empty.");
			return false;
		}
		
		line = line.toUpperCase();
		
    	while(true){

    		if(line.startsWith("OPTIONS")){
    			method = MethodName.Options;
    		}else if(line.startsWith("DESCRIBE")){
    			method = MethodName.Describe;    			
    		}else if(line.startsWith("SETUP")){
    			method = MethodName.Setup;    			
    		}else if(line.startsWith("PLAY")){
    			method = MethodName.Play;    			
    		}else if(line.startsWith("GET_PARAMETER")){
    			method = MethodName.GetParameter;    			
    		}else if(line.startsWith("TEARTOWN")){
    			method = MethodName.TearDown;    			
    		}
    		
    		int start = line.indexOf(":", 0);
    		if(start > 0){
    			headers.put(line.substring(0, start).trim().toUpperCase(), line.substring(start + 1).trim());
    		}
    		
    		Log.i("RTSP", line);
    		if(line.length() == 0)
    			break;
    		
    		line = reader.readLine();
    	}
    	
    	boolean result = false;
    	
    	switch(method){
    	case Options:
    		result = handleOptions(headers, writer);
    		break;
    	case Describe:
    		result = handleDescribe(headers, writer);
    		break;
    	case Setup:
    		result = handleSetup(headers, writer, info);
    		break;
    	case Play:
    		result = handlePlay(headers, writer, info);
    		addSession(info);
    		break;
    	case GetParameter:
    		result = handleGetParameter(headers, writer, info);
    		break;
    	case TearDown:
    		result = handleTearDown(headers, writer, info);
    		removeSession(info);
    		break;
    	default:
    		result = false; 
    		break;
    	}
    	
    	writer.flush();
    	
    	
    	if(method == MethodName.Play && info.isTcp){
    		//read until connection broken.
    		byte[] dummy = new byte[1024]; 
    		while(true){
    			if( -1 == reader.read(dummy)){
    				Log.i("RtspServer", "tcp connecton broken.");
    				break;
    			}
    		}
    	}  	
    	
    	
    	return result;
	}


	private void addSession(RequestInfo info) {

		TcpSender sender = new TcpSender(info.socket);
		
		
		if(info.videoPort > 0 || info.isTcp){
			
			RtpSession session = new RtpSession();
			session.host = info.host;
			session.port = info.videoPort;
			
			if(info.isTcp)
				session.tcpSocket = new TcpChannel(sender, info.videoChannel);
			
			info.videoSession = session;			
		
			videoSessions.add(session);
		}

		if(info.audioPort > 0 || info.isTcp){
			
			RtpSession session = new RtpSession();
			session.host = info.host;
			session.port = info.audioPort;

			if(info.isTcp)
				session.tcpSocket = new TcpChannel(sender, info.audioChannel);

			info.audioSession = session; 
		
			audioSessions.add(session);
		}
		
	}

	private void removeSession(RequestInfo info) {
		videoSessions.remove(info.videoSession);		
		audioSessions.remove(info.audioSession);		
	}

	private void writeCommon(Map<String, String> headers, PrintWriter writer){
		
		writer.write("RTSP/1.0 200 OK\r\n");
		
		String seq =headers.get("CSEQ");
		writer.write("Cseq: " + seq + "\r\n");
		
		writer.write("Server: RtspServer\r\n");		
	}
	
	private boolean handleOptions(Map<String, String> headers, PrintWriter writer) {
		writeCommon(headers, writer);
		writer.write("Public: OPTIONS,DESCRIBE,SETUP,TEARDOWN,PLAY\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");
		
		return true;
	}
	
	private boolean handleDescribe(Map<String, String> headers, PrintWriter writer) {
		
		String localIp = Utility.getLocalIpAddress();
		
		
		String paramSets = "";
		Iterator<byte[]> iter = stream.getSpsPps().iterator();
		while(iter.hasNext()){
			
			byte[] item = iter.next();
			if(item == null)
				continue;
			
			if(paramSets.length()!=0)
				paramSets += ",";
			
			paramSets += Base64.encodeToString (item, 0, item.length, Base64.NO_WRAP);			
		}		
		
		//make android player happy, or it would startup!
		if(paramSets.length() == 0){
			paramSets = "Z01AHuiAbB7zeAiAAAADAIAAABgHixaJ,aOvvIA==";
		}
		
		String sdp = "v=0\r\n"
				   + "o=- 3331435948 1116907222000 IN IP4 " + localIp + "\r\n" 
				   + "c=IN IP4 0.0.0.0\r\n"
				   + "t=0 0\r\n"
				 
		//video		
				   + "m=video 0 RTP/AVP " + videoPayloadType +"\r\n"
				   + "a=rtpmap:" + videoPayloadType + " H264/90000\r\n"
				   + "a=fmtp:125 packetization-mode=1;profile-level-id=4d401e;sprop-parameter-sets=" + paramSets + ";\r\n"
				   + "a=control:rtsp://" + localIp + ":" + port +"/trackID=0\r\n"

		//audio
				   + "m=audio 0 RTP/AVP " + audioPayloadType +"\r\n"
				   + "a=rtpmap:" + audioPayloadType +" mpeg4-generic/" + audioSampleRate + "/" + audioChannel + "\r\n"
				   + "a=fmtp:" + audioPayloadType +" streamtype=5; profile-level-id=15; mode=aac-hbr; config=" + audioConfig +"; " +
				   		"SizeLength=13; IndexLength=3; IndexDeltaLength=3; Profile=1;\r\n"
				   + "a=control:rtsp://" + localIp + ":" + port +"/trackID=1\r\n"
				   ;
	
		
		writeCommon(headers, writer);
		writer.write("Content-Type: application/sdp\r\n");
		writer.write("Content-Length: " + sdp.length() + "\r\n");
		
		writer.write("\r\n");
		
		writer.write(sdp);
		
		return true;
	}
	
	private boolean handleSetup(Map<String, String> headers, PrintWriter writer, RequestInfo info) {
		
		writeCommon(headers, writer);

		int clientPort = 0;
		String line = headers.get("TRANSPORT");
		int start = line.indexOf("client_port=");
		if(start > 0){
			clientPort = Integer.parseInt(line.substring(line.indexOf("=", start) + 1, line.indexOf("-", start)));
		}
		int channelNum = 0;
		info.isTcp = line.indexOf("TCP") > 0;
		if(info.isTcp){
			start = line.indexOf("interleaved");
			if(start > 0){
				start = line.indexOf("=", start) + 1;				
				int end = line.indexOf("-", start);
				channelNum = Integer.parseInt(line.substring(start, end));				
			}
		}						
		
		
		//NOTE: should use url instead.
		if(headers.get("SETUP RTSP").indexOf("TRACKID=0") > 0){
			info.videoPort = clientPort;
			info.videoChannel = channelNum;
		}
		else if(headers.get("SETUP RTSP").indexOf("TRACKID=1") > 0){
			info.audioPort = clientPort;
			info.audioChannel = channelNum;
		}		
		
		if(info.isTcp)
			writer.write("Transport: RTP/AVP/TCP;unicast;interleaved=" + channelNum + "-" + (channelNum + 1) 
					+";ssrc=E257CDCD;mode=play\r\n");
		else
			writer.write("Transport: RTP/AVP/UDP;unicast;client_port=" + clientPort + "-" + (clientPort + 1) 
						+";server_port=57010-57011;ssrc=E257CDCD;mode=play\r\n");
		
		writer.write("Session: 12345678\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");
		
		return true;
	}
	
	private boolean handlePlay(Map<String, String> headers, PrintWriter writer, RequestInfo info) {
		writeCommon(headers, writer);
		writer.write("Session: 12345678\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");
		
		return true;
	}

	private boolean handleGetParameter(Map<String, String> headers, PrintWriter writer, RequestInfo info) {
		writeCommon(headers, writer);
		writer.write("Session: 12345678\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");
		
		return true;
	}
	
	private boolean handleTearDown(Map<String, String> headers, PrintWriter writer, RequestInfo info) {
		writeCommon(headers, writer);
		writer.write("Session: 12345678\r\n");
		writer.write("Content-Length: 0\r\n");
		writer.write("\r\n");

		return false;
	}
}
