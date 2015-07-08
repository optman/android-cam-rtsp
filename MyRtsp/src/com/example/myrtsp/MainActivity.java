package com.example.myrtsp;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import com.optman.rtp.player.Player;
import com.optman.rtp.player.RtspPlayer;
import com.optman.rtp.player.Statistics;
import com.optman.rtp.player.UdpSinkPlayer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.VideoSurfaceView;
import com.optman.rtp.player.*;

public class MainActivity extends Activity {

	//private final static String TAG = "MyRtsp";
	private Player player; 
	
	private int videoReceivePort = 59526;
	private int audioReceivePort = 40272;
	
	private boolean playerStarted = false;

	private Handler statsHandler = new Handler();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ((Button) findViewById(R.id.startBtn)).setText("Stop");
        
        String sources[] =new String[7];
        sources[0]="RTP v 59526 a 40272 (AAC 0x1388 44100HZ stereo)";
        sources[1]="RTP v 50000";
        sources[2]="RTSP(UDP) 192.168.199.99:8554";
        sources[3]="RTSP(TCP) 111.197.87.142:1500(ipcam)";
        sources[4]="RTSP(UDP) 192.168.199.2:8554(video only)";
        sources[5]="RTSP(UDP) 192.168.199.214:9554";
        sources[6]="RTSP(TCP) 192.168.199.214:9554";
        final Spinner s = (Spinner) findViewById(R.id.sources);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sources);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				
				if(playerStarted)
					destroyPlayer();
				
				createPlayer(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        	
        });
        
        s.setSelection(5); 

        
        //((TextView) findViewById(R.id.playerInfo)).setText("Udp Port: V " + videoReceivePort + " A " + audioReceivePort);
        
        
        final Button startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener((new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if(playerStarted){
					destroyPlayer();
					((Button) findViewById(R.id.startBtn)).setText("Start");
				}else{
					createPlayer(s.getSelectedItemPosition());
					((Button) findViewById(R.id.startBtn)).setText("Stop");
				}
				
			}
        })); 
    }
    
    protected void onResume(){
        super.onResume();
    }

	private void createPlayer(int position) {

		MediaFormat videoFormat, audioFormat;
		
    	VideoSurfaceView surfaceView = (VideoSurfaceView) findViewById(R.id.surface_view);
    	switch(position){
    	
    	case 0:
			videoFormat = MediaFormat.createVideoFormat("video/avc", 1024*1024, 0,0, null);
	
			ArrayList<byte[]> audioInitData = new ArrayList<byte[]>(); 
			audioInitData.add(new byte[]{0x13, (byte)0x88});
			audioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 64*1024,	2, 44100, audioInitData);
	    	
			player = new UdpSinkPlayer(videoReceivePort, audioReceivePort, surfaceView, new Handler(this.getMainLooper()), 2);    
	    	player.setVideoFormat(videoFormat);    	
	    	player.setAudioFormat(audioFormat);

	    	player.setJitterBuffer(3*1000*1000);
	    	player.start();
	    	break;
	    	
    	case 1:
			videoFormat = MediaFormat.createVideoFormat("video/avc", 1024*1024, 0,0, null);
			
			player = new UdpSinkPlayer(50000, 0, surfaceView, new Handler(this.getMainLooper()), 1);    
	    	player.setVideoFormat(videoFormat);    	
	    	//player.setJitterBuffer(30*1000);
	    	player.setJitterBuffer(0);
	    	player.start();
    		break;
    		
    	case 2:
	    	player = new RtspPlayer("rtsp://192.168.199.99:8554/", surfaceView, new Handler(this.getMainLooper()), 2, false);
	    	player.setJitterBuffer(3*1000*1000);
			player.start();
			break;
			
    	case 3:
	    	player = new RtspPlayer("rtsp://111.197.87.142:1500/", surfaceView, new Handler(this.getMainLooper()), 1, true);
	    	player.setJitterBuffer(5*1000*1000);
			player.start();
			break;
			
    	case 4:
	    	player = new RtspPlayer("rtsp://192.168.199.2:8554/", surfaceView, new Handler(this.getMainLooper()), 1, false);
	    	player.setJitterBuffer(30*1000);
			player.start();
			break;

    	case 5:
	    	player = new RtspPlayer("rtsp://192.168.199.214:9554/", surfaceView, new Handler(this.getMainLooper()), 2, false);
	    	player.setJitterBuffer(100*1000);
			player.start();
			break;
			
    	case 6:
	    	player = new RtspPlayer("rtsp://192.168.199.214:9554/", surfaceView, new Handler(this.getMainLooper()), 2, true);
	    	player.setJitterBuffer(100*1000);
			player.start();
			break;
    	}
    	
		playerStarted = true;	
		
		startStatsTimer();
	}   
	
	private void destroyPlayer(){

		if(player != null){
			player.stop();
			player = null;
		}
		
		playerStarted = false;
	}

	private void startStatsTimer() {
		statsHandler.postDelayed(new Runnable(){

			@Override
			public void run() {
				
				showStats();
				
				if(playerStarted)
					startStatsTimer();
			}
			
		}, 1000);
	}

	private void showStats() {
		
		if(player == null)
			return;
		
		TextView textView = (TextView) findViewById(R.id.statistics);

		Statistics st = player.getStats();
		
		textView.setText( "\r\n" +
						  "v " + st.videoFrameCount + " a " + st.audioFrameCount  + " drop " + st.transportDropPacketCount + 
						  " sync " + st.syncframeCount + " render drop " + st.renderDropframeCount +
						  " state " + st.playbackState + "\r\n");   	
		
	}


        
    protected void onStop(){

    	destroyPlayer();
    	
    	super.onStop();
    }
    
    protected void onDestroy() {
    	android.os.Process.killProcess(android.os.Process.myPid());
    	super.onDestroy();
    }

    

}
