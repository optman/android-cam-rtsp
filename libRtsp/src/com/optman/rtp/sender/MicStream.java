package com.optman.rtp.sender;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.optman.rtp.sender.RtpAacStream;
import com.optman.rtp.sender.RtpSocket;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaMuxer.OutputFormat;
import android.os.Environment;
import android.util.Log;


@SuppressWarnings("deprecation")
public class MicStream extends RtpAacStream{

    private AudioRecord record;
	private MediaCodec mMediaCodec;
	private int sampleRate;	
	private int channelNum;
	private boolean stoped;
	private Thread thread;
	private byte[] audioConfig;
	private long baseTimeUs;

	public MicStream(long baseTimeUs, int sampleRate, int channelNum, RtpSocket socket) {
		super(sampleRate, socket);
		this.baseTimeUs = baseTimeUs;
		this.sampleRate = sampleRate;
		this.channelNum =channelNum;
	}

    public void start() throws IOException{
    	
    	stoped = false;
    	
    	int channel = channelNum > 1 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
    	int format = AudioFormat.ENCODING_PCM_16BIT; 
    	final int bufSize = AudioRecord.getMinBufferSize(sampleRate, channel, format);   	
    	
    	
        MediaFormat mAudioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channelNum);
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
                

        mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        mMediaCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start(); 

       
    	record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,channel, format, bufSize); 
    	record.startRecording();
    	
    	thread = new Thread(new Runnable(){

			@Override
			public void run() {
				
				ByteBuffer data = ByteBuffer.allocateDirect(bufSize);
				
				ByteBuffer[] inBuffers = mMediaCodec.getInputBuffers();
				ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
				BufferInfo info = new BufferInfo();
				
				while(!stoped){
					data.clear();
					int readSize = record.read(data, data.capacity());
					//Log.i("Audio", "readSize:" + readSize);
					
					if(readSize > 0){
						int inBufferIndex = mMediaCodec.dequeueInputBuffer(100*1000);
						if(inBufferIndex >= 0){
							
							long ptsUs = System.nanoTime()/1000 - baseTimeUs;
							
							ByteBuffer buf = inBuffers[inBufferIndex];
							buf.clear();
							buf.put(data);
							
							mMediaCodec.queueInputBuffer(inBufferIndex, 0, readSize, ptsUs, 0);
						}
					}
					
					while(!stoped){
						int outBufferIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
						if(outBufferIndex >= 0){
							ByteBuffer buf = outBuffers[outBufferIndex];
							
							if(info.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
							{
								buf.position(info.offset);
								buf.limit(info.offset + info.size);
								
								try {
									addAU(buf, info.size, info.presentationTimeUs);
								} catch (IOException e) {
									e.printStackTrace();
								}								
								
							}

							mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
						}else if(outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
							MediaFormat format = mMediaCodec.getOutputFormat(); 
			            	ByteBuffer buf= format.getByteBuffer("csd-0");
			            	audioConfig = new byte[buf.capacity()];
			            	buf.get(audioConfig);
			            	
			            	Log.i("MicStream", audioConfig.toString());

						}else if(outBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
							outBuffers = mMediaCodec.getOutputBuffers();
						}else{
							break;
						}
						
					}					
					
				}
				
			}
    		
    	});
    	
    	thread.start();
    }
    
    public void stop(){
    	stoped = true;
    	
    	try {
			thread.join(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	
    	mMediaCodec.stop();
    	mMediaCodec.release();
    	mMediaCodec = null;

    	record.stop();
    	record.release();
    	record = null;
    }
    
    public byte[] getAudioConfig(){
    	return audioConfig;
    }
	
}
