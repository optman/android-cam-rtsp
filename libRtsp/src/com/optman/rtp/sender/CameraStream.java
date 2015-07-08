package com.optman.rtp.sender;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.optman.rtp.sender.RtpAvcStream;
import com.optman.rtp.sender.RtpSocket;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.util.Log;


@SuppressWarnings("deprecation")
public class CameraStream extends RtpAvcStream{

	final static String TAG = "CameraStream"; 
	
	private Camera mCamera;
	private MediaCodec mMediaCodec;
	private ByteBuffer[] sps_pps;
	private Thread thread;
	private boolean stoped;
	private long baseTimeUs;
	
	
	public CameraStream(long baseTimeUs, Camera c, RtpSocket socket) {
		super(socket);
		
		this.baseTimeUs = baseTimeUs;
		mCamera = c;
	}

    public void start() {
    	
    	stoped = false;
    	
    	final Size size = mCamera.getParameters().getPreviewSize();
    	
    	final int imageBufferSize = size.width*size.height*ImageFormat.getBitsPerPixel(ImageFormat.NV21)/8;
		final int yuvImageBufferSize = size.width*size.height*ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)/8;
		
    	final byte[] yuvImageBuffer = new byte[yuvImageBufferSize];
    	
		try {
			mMediaCodec = MediaCodec.createEncoderByType("video/avc");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", size.width, size.height);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2*1000*1000);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mMediaCodec.start();
		

		Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				long ptsUs = System.nanoTime()/1000 - baseTimeUs;
				try {
					int bufferIndex = mMediaCodec.dequeueInputBuffer(500000);
					if (bufferIndex>=0) {

						ByteBuffer buf = inputBuffers[bufferIndex]; 
						buf.clear();
						
						if (data == null) 
							Log.e(TAG,"Symptom of the \"Callback buffer was to small\" problem...");
						else {
							YV12toYUV420PackedSemiPlanar(data, yuvImageBuffer, size.width, size.height);
							buf.put(yuvImageBuffer);
							//buf.put(data);
						}
						
						mMediaCodec.queueInputBuffer(bufferIndex, 0, buf.position(), ptsUs, 0);
					} else {
						Log.e(TAG,"No buffer available !");
					}
				} finally {
					mCamera.addCallbackBuffer(data);
				}				
			}
		};
		
		for (int i=0;i<10;i++) mCamera.addCallbackBuffer(new byte[imageBufferSize]);
		mCamera.setPreviewCallbackWithBuffer(callback);
		
		
		thread = new Thread(new Runnable(){

			@Override
			public void run() {
				
				ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
				BufferInfo info = new BufferInfo();				
				while(!stoped){
					int bufferIndex = mMediaCodec.dequeueOutputBuffer(info, 200*1000);
					if(bufferIndex >= 0){
						ByteBuffer buf = outBuffers[bufferIndex];

						buf.position(info.offset);
						buf.limit(info.offset + info.size);
						
						addSample(buf, info.size, info.presentationTimeUs, info.flags);						
						
												
						mMediaCodec.releaseOutputBuffer(bufferIndex, false);
					}else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
						outBuffers = mMediaCodec.getOutputBuffers();						 
					}else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){ 
					}else{
						//throw new RuntimeException();
					}
				}
			}
			
		});
		
		thread.start();
		
	}
    
    public void stop(){
    	
    	
    	mCamera.setPreviewCallbackWithBuffer(null);    	
    	
    	stoped = true;
    	
    	try {
			thread.join(2*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	mMediaCodec.stop();
    	mMediaCodec.release();
    	mMediaCodec = null;
    	
    }

    public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height) {
        /* 
         * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
         * We convert by putting the corresponding U and V bytes together (interleaved).
         */
        final int frameSize = width * height;
        final int qFrameSize = frameSize/4;

        System.arraycopy(input, 0, output, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            output[frameSize + i*2] = input[frameSize + i + qFrameSize]; // Cb (U)
            output[frameSize + i*2 + 1] = input[frameSize + i]; // Cr (V)
        }
        return output;
    }
    
    private void addSample(ByteBuffer buf, int size, long timeUs, int flags){

    	try{
    	
    	int startCode = buf.getInt();		//skip start code.
    	if(startCode != 0x1000000)
    		throw new RuntimeException();
    	
   		size -= 4;
    	

    	if(flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
    		sps_pps = new ByteBuffer[2];
    		
    		int pos = findStartcode(buf);
    		if(pos < 0)
    			throw new RuntimeException();    		
    		
    		int first_sps_pps_size = pos - buf.position();
    		
        	sps_pps[0] = ByteBuffer.allocate(first_sps_pps_size);
        	sps_pps[0].mark();
        	buf.get(sps_pps[0].array());

        	buf.getInt();	//skip start code
        	
        	sps_pps[1] = ByteBuffer.allocate(size - first_sps_pps_size - 4);
        	sps_pps[1].mark();
        	
        	buf.get(sps_pps[1].array());
        	
        	return;

    	}else if(flags == MediaCodec.BUFFER_FLAG_SYNC_FRAME){
    		sps_pps[0].reset();
    		sps_pps[1].reset();
    		addNalu(sps_pps[0].array(), 0, sps_pps[0].capacity(), timeUs);
    		addNalu(sps_pps[1].array(), 0, sps_pps[1].capacity(), timeUs);
    	}
    	
		addNalu(buf, size, timeUs);
    	
    	}catch(IOException e){
    		
    	}
    	
    	
    }

	private int findStartcode(ByteBuffer buf) {
		
		buf.mark();
		
		int zeroCount = 0;

		int pos = -1;
		
		while(buf.hasRemaining()){
			byte b = buf.get();
			
			if(b == 0){
				zeroCount++;
				continue;
				
			}else if( b == 1){
				if(zeroCount >= 2){
					pos = buf.position() - zeroCount - 1;
					break;
				}
			}

			zeroCount = 0;			
		}		
		
		buf.reset();
		
		return pos;		
	}
 
}
