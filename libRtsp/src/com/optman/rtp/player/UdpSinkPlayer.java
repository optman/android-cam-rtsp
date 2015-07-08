package com.optman.rtp.player;

import java.net.SocketException;

import android.os.Handler;

import com.optman.rtp.player.Player;
import com.optman.rtp.player.SimplePlayer;
import com.optman.rtp.player.Statistics;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.VideoSurfaceView;
import com.optman.rtp.receiver.RtpAacStream;
import com.optman.rtp.receiver.RtpAvcStream;
import com.optman.rtp.receiver.RtpStream;
import com.optman.rtp.receiver.Sample;
import com.optman.rtp.receiver.SampleHandler;
import com.optman.rtp.receiver.UdpServer;

public class UdpSinkPlayer implements Player{

	//private final static String TAG = "UdpSinkPlayer"; 
	
	private int videoReceivePort;
	private int audioReceivePort;

	private UdpServer videoReceiver;
	private UdpServer audioReceiver;	
	
	private RtpStream videoStream;
	private RtpStream audioStream;
	
	private SimplePlayer player;
	
	private int audioSampleRate;
	
	private int sourceCount;
	

	public UdpSinkPlayer(int videoReceivePort, int audioReceivePort, VideoSurfaceView view, Handler handler, int sourceCount) {
		
		player = new SimplePlayer(view, handler, sourceCount);

		this.videoReceivePort = videoReceivePort;
		this.audioReceivePort = audioReceivePort;
		this.sourceCount = sourceCount;
	}


	public void setVideoFormat(MediaFormat format){
		player.setVideoFormat(format);
	}

	
	public void setAudioFormat(MediaFormat format){
		player.setAudioFormat(format);
		if(format != null)
			audioSampleRate = format.sampleRate;
	}

	public void start(){

		player.start();
		
		try {
			startRtp();
		} catch (SocketException e) {
			e.printStackTrace();
		}       
		
	}
	
	public void stop(){
		
		stopRtp();
		
		player.stop();
		player = null;
	}
	
    void startRtp() throws SocketException{
    	
   	
    	videoStream = new RtpAvcStream(new SampleHandler(){

			@Override
			public void onSample(Sample sample) {
				player.onVideoSample(sample);
			}
    		
    	}, getStats());    	
    	videoReceiver = new UdpServer(videoReceivePort, videoStream);
    	videoReceiver.open();    	
    	
    	if(sourceCount > 1){
    	
	    	audioStream = new RtpAacStream(audioSampleRate, new SampleHandler(){
	
				@Override
				public void onSample(Sample sample) {
					player.onAudioSample(sample);
				}
	    		
	    	}, getStats());
	    	audioReceiver = new UdpServer(audioReceivePort, audioStream);
	    	audioReceiver.open();
    	
    	}
    }
    
    void stopRtp(){
    	
    	if(videoStream != null){
    		videoStream.close();
    		videoStream = null;
    	}
    	if(videoReceiver != null){
    		videoReceiver.close();
    		videoReceiver = null;
    	}
    	
    	if(audioStream != null){
    		audioStream.close();
    		audioStream = null;
    	}
    	if(audioReceiver != null){
    		audioReceiver.close();
    		audioReceiver = null;
    	}
    	
    }


	@Override
	public void addVideoPacket(byte[] data, int dataSize) {
	}

	@Override
	public void addAudioPacket(byte[] data, int dataSize) {
	}


	@Override
	public Statistics getStats() {
		return player.stats;
	}	

	@Override
	public void setJitterBuffer(long timeUs) {
		player.setJitterBuffer(timeUs);		
	}	

}
