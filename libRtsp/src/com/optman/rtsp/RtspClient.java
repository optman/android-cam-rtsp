package com.optman.rtsp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.optman.rtsp.RtspClientListener;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class RtspClient implements Runnable {

	private RtspClientListener listener;
	
	private Socket s;
	private String session;
	private String videoTrackUrl;
	private String audioTrackUrl;

	public int sampleRate;
	public List<byte[]> sps_pps;
	public byte[] audioConfig; 
	public int channel; 
	
	private String rtspUrl;
	private boolean playVideo;
	private int receivePort;	
	
	private boolean overTcp;

	private DataInputStream reader;

	private PrintWriter writer;
	
	Thread thread;
	
	
	public RtspClient(String rtspUrl, boolean playVideo, int receivePort,RtspClientListener listener){
		this.rtspUrl = rtspUrl;
		this.playVideo = playVideo;
		this.receivePort = receivePort;		
		this.listener = listener;
		
		this.overTcp = receivePort == 0;
	}
	
	public void start(){
		thread = new Thread(this);
		thread.start();
	}
	
	public void stop(){
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {

		try {
			rtspPlay();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	@SuppressWarnings("deprecation")
	private void request(String content) throws UnknownHostException, IOException{
    	
    	Log.i("RTSP", content);
    	
    	writer.write(content);
    	writer.flush();
    	
    	
    	int contentLength = 0;
    	String content_base = "";

		String line = reader.readLine();
    	while(line != null){

    		if(line.startsWith("Session")){
    			int start = line.indexOf(':');
    			int end = line.indexOf(';');
    			if(end != -1)
    				session  = line.substring(start + 1, end);
    			else
    				session  = line.substring(start + 1);
    				
    			session = session.trim();    			
    		}

    		if(line.startsWith("Content-Length") || line.startsWith("Content-length")){
    			int start = line.indexOf(':');
    			contentLength = Integer.parseInt(line.substring(start + 1).trim());    			    			
    		}

    		if(line.startsWith("Content-Base")){
    			int start = line.indexOf(':');
    			content_base = line.substring(start + 1);    			    			
    		}

    		
    		Log.i("RTSP", line);
    		if(line.length() == 0)
    			break;
    		
    		line = reader.readLine();
    	}    	    	
    	
    	if(contentLength > 0){
       		byte[] buf = new byte[contentLength];
    		int offset = 0;
    		while(offset < contentLength){
    			offset += reader.read(buf, offset, contentLength - offset);
    		}
    		
    		String body = new String(buf);
    		Log.i("RTSP", body);
    		

    		if(body.indexOf("m=video") > 0){
    			int start = body.indexOf(":", body.indexOf("a=control", body.indexOf("m=video")));
    			videoTrackUrl = body.substring(start + 1, body.indexOf("\n", start)).trim();
    			if(!videoTrackUrl.startsWith("rtsp:"))
    				videoTrackUrl = content_base + videoTrackUrl; 
    			   			
    			
    			
    			start = body.indexOf("=", body.indexOf("sprop-parameter-sets")); 
    			int end = body.indexOf(",", start);
    		
    			sps_pps = new ArrayList<byte[]>();   			
    			
    			if(start > 0 && end > 0){
    				sps_pps.add(Base64.decode(body.substring(start + 1, end), 0));
    				int xend = body.indexOf(";", end); 
    				if( xend < 0)
    					xend = body.indexOf("\n", end);
    				
    				sps_pps.add(Base64.decode(body.substring(end + 1, xend), 0));
    					
    			}
    		}
    		
    		if(body.indexOf("m=audio") > 0){
    			int start = body.indexOf(":", body.indexOf("a=control", body.indexOf("m=audio")));
    			audioTrackUrl = body.substring(start + 1, body.indexOf("\n", start)).trim();
    			if(!videoTrackUrl.startsWith("rtsp:"))
    				audioTrackUrl = content_base + audioTrackUrl;
    			
    			start = body.indexOf("/", body.indexOf("rtpmap", body.indexOf("m=audio")));
    			
    			int end = body.indexOf("/", start + 1);
    			if( end - start < 10){
    				sampleRate = Integer.parseInt(body.substring(start + 1, end));    			
    				channel = Integer.parseInt(body.substring(end + 1, body.indexOf("\r\n", end + 1)));
    			}else{
    				end = body.indexOf("\r\n", start + 1); 
    				sampleRate = Integer.parseInt(body.substring(start + 1, end));
    				channel = 1;
    			}
    			
    			start = body.indexOf("=", body.indexOf("config", body.indexOf("m=audio")));
    			String config = body.substring(start + 1, body.indexOf(";", start));
    			
    			audioConfig = hexStringToByteArray(config);
    			
    		}
    		
    		
    	}   	
    	
	}

	private String getTrackUrl(){
		return playVideo ? videoTrackUrl : audioTrackUrl;
	}
	
    private void rtspPlay() throws UnknownHostException, IOException, InterruptedException {

    	Uri uri = Uri.parse(rtspUrl);
    	
    	s = new Socket(uri.getHost(), uri.getPort());
    	reader = new DataInputStream(s.getInputStream());
    	writer = new PrintWriter(s.getOutputStream());

    	int CSeq = 1;

    	String content;
    	
    	content= "DESCRIBE " + rtspUrl + " RTSP/1.0\n"
				+ "CSeq: " + CSeq++ + "\n"
				+ "User-Agent: TestRtsp\n"
				+ "\n";
    	
    	request(content);

    	content = "SETUP " + getTrackUrl()+ " RTSP/1.0\n"
				+ "CSeq: " + CSeq++ + "\n"
				+ "User-Agent: TestRtsp\n";    	
    	if(overTcp){
    		content += "Transport: RTP/AVP/TCP;unicast;interleaved=0-1\n";
    	}else{
    		content += "Transport: RTP/AVP;unicast;client_port=" + receivePort+ "-" + (receivePort + 1) + "\n";
    	}
    	content += "\n";

    	request(content);
    	
    	content = "PLAY " + getTrackUrl() + " RTSP/1.0\n"
				+ "CSeq: " + CSeq++ + "\n"
				+ "User-Agent: TestRtsp\n"
				+ "Session: " + session + "\n"
    			+ "\n";    	

    	request(content);

    	if(listener != null)
    		listener.onReady(this);

    	while(true){
    		
    		if(thread.isInterrupted())
    			break;
    		
	    	if(overTcp){
	    		
	    		int magicNum = reader.readByte();
	    		int channelNum = reader.readByte();
	    		
	    		if(magicNum != 36)
	    			throw new RuntimeException();
	    		
	    		int dataSize = reader.readShort();
	    		byte[] data = new byte[dataSize];
    			reader.readFully(data);
    			
    			if(channelNum == 0){
	    			if(listener != null)
	    				listener.onRtpPacket(data, dataSize);
    			}else{
    	    		//Log.i("RTSP", new String(data));
    	    	}
    			
    			
	    	}else{
		    		Thread.sleep(30*1000);    		

		    		content = "GET_PARAMETER " + getTrackUrl() + " RTSP/1.0\n"
		    				+ "CSeq: " + CSeq++ + "\n"
		    				+ "User-Agent: TestRtsp\n"
		    				+ "Session: " + session + "\n"
		    				+ "\n";    	
		
		    		request(content);
	    	}
    	}    	    	
    	
    	//s.close();
    	
    }
	

}
