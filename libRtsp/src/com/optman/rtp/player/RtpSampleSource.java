package com.optman.rtp.player;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import android.util.Log;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.MediaFormatHolder;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackInfo;
import com.google.android.exoplayer.TrackRenderer;
import com.optman.rtp.receiver.Sample;


public class RtpSampleSource implements SampleSource {

	private static final String TAG = "RtpSampleSource";
	
	private boolean formatSent = false;
	
//	private List<byte[]> initializationData;
	
	private Queue<Sample> samples = new ArrayDeque<Sample>();
	
	private long durationUs = TrackRenderer.UNKNOWN_TIME_US;
	
	private MediaFormat format;
	
	public RtpSampleSource(){
	}
	
	
	public synchronized void setFormat(MediaFormat format){
		this.format = format;
	}
	
	public synchronized MediaFormat getFormat(){
		return this.format;
	}
	
//	public synchronized void setInitializationData(List<byte[]> data){
//		this.initializationData = data;		
//	}
	
	public synchronized  void addSample(Sample sample){
		samples.add(sample);
	}	
	
	@Override
	public synchronized boolean prepare() throws IOException {
		
		return /*initializationData != null && */format != null;
	}

	@Override
	public int getTrackCount() {
		return 1;
	}

	@Override
	public TrackInfo getTrackInfo(int track) {
		return new TrackInfo(format.mimeType, durationUs);
	}

	@Override
	public void enable(int track, long timeUs) {
	}

	@Override
	public void disable(int track) {
	}

	@Override
	public boolean continueBuffering(long playbackPositionUs) throws IOException {
		return true;
	}

	@Override
	public synchronized  int readData(int track, long playbackPositionUs,
			MediaFormatHolder formatHolder, SampleHolder sampleHolder,
			boolean onlyReadDiscontinuity) throws IOException {

		if(onlyReadDiscontinuity){
			return NOTHING_READ;
		}

		if(!formatSent){
			
			formatHolder.format = this.format;
			formatSent = true;
			return FORMAT_READ;
		}
		
		if(samples.peek() == null)
			return NOTHING_READ;
		
		
		if (sampleHolder.data != null){ 

			Sample sample = samples.poll();
			if(sample != null){
				sampleHolder.size = sample.data.length;
				sampleHolder.data.put(sample.data);
	
				sampleHolder.timeUs = sample.timestampUs;
				sampleHolder.flags = sample.keyframe ? android.media.MediaExtractor.SAMPLE_FLAG_SYNC : 0;
	
				if(sample.isVideo && sample.keyframe)
					Log.i(TAG, "video keyframe " +" timeS: " + sampleHolder.timeUs/1000000 );
			}

		}else{
			sampleHolder.size = 0;			
		}

		return SAMPLE_READ;
	}

	@Override
	public void seekToUs(long timeUs) {
	}

	@Override
	public long getBufferedPositionUs() {
		return TrackRenderer.UNKNOWN_TIME_US;
	}

	@Override
	public void release() {
		
	}
	
}
