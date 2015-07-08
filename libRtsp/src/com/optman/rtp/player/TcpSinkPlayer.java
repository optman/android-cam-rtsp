package com.optman.rtp.player;

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

public class TcpSinkPlayer implements Player {

	private RtpStream videoStream;
	private RtpStream audioStream;
	
	private SimplePlayer player;
	
	private int audioSampleRate; 
	

	public TcpSinkPlayer(VideoSurfaceView view, Handler handler, int sourceCount) {
		
		player = new SimplePlayer(view, handler, sourceCount);
	}


	public void setVideoFormat(MediaFormat format){
		player.setVideoFormat(format);
	}

	
	public void setAudioFormat(MediaFormat format){
		player.setAudioFormat(format);
		audioSampleRate = format.sampleRate;
	}

	public void start(){

		player.start();
		
		startRtp();		
	}
	
	public void stop(){
		
		stopRtp();
		
		player.stop();
		player = null;
	}
	
    void startRtp(){
   	
		videoStream = new RtpAvcStream(new SampleHandler(){
		
			@Override
			public void onSample(Sample sample) {
				player.onVideoSample(sample);
			}
			
		}, getStats());    	
		
		audioStream = new RtpAacStream(audioSampleRate, new SampleHandler(){
		
			@Override
			public void onSample(Sample sample) {
				player.onAudioSample(sample);
			}
			
		}, getStats());
    }
    
    void stopRtp(){
    	
    	if(videoStream != null)
    		videoStream.close();
    	
    	if(audioStream != null)
    		audioStream.close();
    	
    }


	@Override
	public void addVideoPacket(byte[] data, int dataSize) {
		videoStream.onRtp(data, dataSize);
	}


	@Override
	public void addAudioPacket(byte[] data, int dataSize) {
		audioStream.onRtp(data, dataSize);
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
