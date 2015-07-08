package com.example.liveview;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.optman.rtp.sender.CameraStream;
import com.optman.rtp.sender.MicStream;
import com.optman.rtp.sender.RtpAacStream;
import com.optman.rtp.sender.RtpAvcStream;
import com.optman.rtp.sender.RtpSocket;
import com.optman.rtp.sender.RtpUdp;
import com.optman.rtsp.TcpChannel;
import com.optman.rtsp.TcpSender;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


public class LiveView extends Activity {

	private Camera mCamera;
    private CameraPreview mPreview;
	private CameraStream videoStream;
	private MicStream audioStream;
	private RtpSocket videoSocket;
	private RtpSocket audioSocket;
	
	private GestureDetectorCompat mDetector; 
	private int cameraIndex;
	
    private static long baseTimeUs = System.nanoTime()/1000;
    private static boolean overTcp = false;
    
	private TcpSender tcpSender; 

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_view);

		mDetector = new GestureDetectorCompat(this, new MyGestureListener(this));

        Button protoBtn = (Button) findViewById(R.id.flip_protocol);
        protoBtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
					overTcp = !overTcp;
					restart();
			}
        });
		
        
        open();		
    }
	    
    @Override
    protected void onPause() {
        super.onPause();

        close();
    }

    
	private void restart() {
		close();					
		open();
	}
    
    private void open() {
    	
        Button protoBtn = (Button) findViewById(R.id.flip_protocol);
		if(overTcp)
			protoBtn.setText("TCP");
		else 
			protoBtn.setText("UDP");		

    	
		// Create an instance of Camera
        mCamera = getCameraInstance(cameraIndex);
    	
        Parameters p = mCamera.getParameters();
    	p.setPreviewFormat(ImageFormat.YV12);
    	p.setPreviewSize(1280, 720);
    	mCamera.setParameters(p);
    	
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
     
        
        (new Thread(new Runnable(){

			@Override
			public void run() {
				start();
			}
        	
        })).start();
	}
    
    private void start(){
    	
    	if(!overTcp){
			videoSocket = new RtpUdp("127.0.0.1", 59526, false);
			audioSocket = new RtpUdp("127.0.0.1", 40272, false);
		}else{
			try {
		    	tcpSender = new TcpSender(new Socket("127.0.0.1", 8001));
				videoSocket = new TcpChannel(tcpSender, 0);
				audioSocket = new TcpChannel(tcpSender, 1);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
				
		
		videoStream = new CameraStream(baseTimeUs, mCamera, videoSocket);
		videoStream.start();

		
		try {
			audioStream = new MicStream(baseTimeUs, 44100, 2, audioSocket);
			audioStream.start();
		} catch (IOException e) {
			e.printStackTrace();
		}    
		
    }

    protected void close(){
    	
    	if(mCamera == null)
    		return;

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeAllViews();

        videoStream.stop();
        videoSocket.close();
    	
    	audioStream.stop();
		audioSocket.close();
		
		if(tcpSender != null){
			tcpSender.close();
			tcpSender = null;
		}
        
        mCamera.stopPreview();
        mCamera.release();
      
        mCamera = null;
    }
    
	/** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(int cameraIndex){
        Camera c = null;
        try {
            c = Camera.open(cameraIndex); // attempt to get a Camera instance
            
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    
    public void flipCamera(){
    	cameraIndex = (cameraIndex + 1)%Camera.getNumberOfCameras();

    	restart();
    }    
    

    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
    
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
    	
    	LiveView parent;
    	
    	public MyGestureListener(LiveView parent){
    		this.parent = parent;
    	}
    	
        @Override
        public boolean onDown(MotionEvent event) { 
            return true;
        }
        
        public boolean onSingleTapConfirmed(MotionEvent e){
        	
        	this.parent.flipCamera();
        	return true;
        }
    }

}
