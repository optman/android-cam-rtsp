package com.optman.rtp.player;

import android.os.Handler;

import com.optman.rtp.player.RtpPlayer;
import com.optman.rtp.player.RtpSampleSource;
import com.optman.rtp.player.Statistics;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.VideoSurfaceView;
import com.optman.rtp.receiver.Sample;

public class SimplePlayer {

	//private static final String TAG = "SimplePlayer";
	
	private RtpPlayer player;
	
	private int sourceCount;

	boolean firstBuffered = false; 
	private long firstVideoSampleTimeUs = 0;
	private long firstAudioSampleTimeUs = 0;
	private long jitterBufferUs = 2*1000*1000;	

	private RtpSampleSource videoSource;
	private RtpSampleSource audioSource;
	
	private MediaFormat videoFormat;
	private MediaFormat audioFormat;	

	public Statistics stats;

	public SimplePlayer(VideoSurfaceView view, Handler handler, int sourceCount){

		this.sourceCount = sourceCount; 
		
		stats = new Statistics();
		
		player = new RtpPlayer(sourceCount, view, handler, stats);
	}	

	public void setVideoFormat(MediaFormat format){
		this.videoFormat = format;
	}
	
	public void setAudioFormat(MediaFormat format){
		this.audioFormat = format;
	}
	
	public void setJitterBuffer(long timeUs){
		this.jitterBufferUs = timeUs;
	}
	
	public void start(){
    	videoSource = new RtpSampleSource();
    	videoSource.setFormat(videoFormat);

    	audioSource = new RtpSampleSource();
    	audioSource.setFormat(audioFormat);    	
	}
	
	public void stop(){
		player.close();
		player = null;
	}
	
	protected void onVideoSample(Sample sample){
		
		if(videoSource == null)
			return;
		
		//ignore sample time stamp.
		if(this.jitterBufferUs == 0)
			sample.timestampUs = System.nanoTime()/1000;		
		
		//Log.i(TAG, "video sample " + sample.timestampUs);

		if(firstVideoSampleTimeUs == 0)
			firstVideoSampleTimeUs = sample.timestampUs;
			

		if(!firstBuffered &&  sample.timestampUs > firstAudioSampleTimeUs  //video should catch up with audio. 
				&& (sample.timestampUs  - firstVideoSampleTimeUs > jitterBufferUs))  //video should have enough buffer.
		{
//			if(sourceCount > 1 && (firstVideoSampleTimeUs - firstAudioSampleTimeUs) < -10*1000000)
//				throw new RuntimeException();
	
			
			firstBuffered = true;
			readyToPlay();
		}		
		
		//only video
		if(sourceCount == 1){
			sample.timestampUs -= firstVideoSampleTimeUs;
			sample.timestampUs += jitterBufferUs;
		}
		
		videoSource.addSample(sample);
	
	}

	public void onAudioSample(Sample sample){
		
		if(audioSource == null)
			return;
		
		//Log.i(TAG, "audio sample " + sample.timestampUs);
		
		if(firstAudioSampleTimeUs == 0){
			firstAudioSampleTimeUs = sample.timestampUs;

			//only audio
			if(sourceCount == 1 )
				readyToPlay();
		}
		
		audioSource.addSample(sample);
	
	}
	
	protected void readyToPlay(){

    	if(sourceCount > 1)
    		player.prepare(videoSource, audioSource);
    	else
    		player.prepare(videoSource);
		
		player.play();	
	}
}
