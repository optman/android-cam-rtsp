package com.optman.rtp.player;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

import com.optman.rtp.player.Player;
import com.optman.rtp.player.Statistics;
import com.optman.rtp.player.TcpSinkPlayer;
import com.optman.rtp.player.UdpSinkPlayer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.VideoSurfaceView;
import com.optman.rtsp.RtspClient;
import com.optman.rtsp.RtspClientListener;

public class RtspPlayer implements Player{

	private int videoReceivePort = 40300;
	private int audioReceivePort = 40302;
	
	private Player player;
	private RtspClient rtspVideo, rtspAudio;
	
	int readyCount;
	
	
	public RtspPlayer(String url, VideoSurfaceView view, Handler handler, final int sourceCount, boolean overTcp) {
		
		if(overTcp)
			player = new TcpSinkPlayer(view, handler, sourceCount);
		else
			player = new UdpSinkPlayer(videoReceivePort, audioReceivePort, view, handler, sourceCount);

		rtspVideo = new RtspClient(url, true, (overTcp ? 0 : videoReceivePort), new RtspClientListener(){

			@Override
			public void onReady(RtspClient client) {

				List<byte[]> initData = new ArrayList<byte[]>();
				for(int i = 0; client.sps_pps!= null && i < client.sps_pps.size(); i++){
					byte[] src = client.sps_pps.get(i);
					byte[] dst = new byte[src.length + 4];
					dst[0] = 0;
					dst[1] = 0;
					dst[2] = 0;
					dst[3] = 1;
					System.arraycopy(src, 0, dst, 4, src.length);		
					
					initData.add(dst);
				}	
				
				MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1024*1024, 0,0, initData);

				player.setVideoFormat(format);
				
				onRtspReady(sourceCount);				
			}

			@Override
			public void onRtpPacket(byte[] data, int dataSize) {
				if(player != null)
					player.addVideoPacket(data, dataSize);
			}});
		
		if(sourceCount > 1)
			rtspAudio = new RtspClient(url, false, (overTcp ? 0 : audioReceivePort), new RtspClientListener(){

				@Override
				public void onReady(RtspClient client) {

					if(client.audioConfig == null)
						return;
					
					List<byte[]> initData= new ArrayList<byte[]>();
					initData.add(client.audioConfig);
					
					MediaFormat format = MediaFormat.createAudioFormat("audio/mp4a-latm", 64*1024,	client.channel, client.sampleRate, initData);
					
					player.setAudioFormat(format);
					
					onRtspReady(sourceCount);					
				}

				@Override
				public void onRtpPacket(byte[] data, int dataSize) {
					if(player != null)
						player.addAudioPacket(data, dataSize);
				}});
	}
	
	private void onRtspReady(int sourceCount){
		readyCount++;
		
		if(readyCount == sourceCount)
			player.start();
		
	}

	@Override
	public void start() {

		rtspVideo.start();
		if(rtspAudio != null)
			rtspAudio.start();
		
	}

	@Override
	public void stop() {

		player.stop();
		player = null;

		rtspVideo.stop();
		if(rtspAudio != null)
			rtspAudio.stop();
	}

	@Override
	public void setVideoFormat(MediaFormat format) {
	}

	@Override
	public void setAudioFormat(MediaFormat format) {
	}

	@Override
	public void addVideoPacket(byte[] data, int dataSize) {
	}

	@Override
	public void addAudioPacket(byte[] data, int dataSize) {
	}

	@Override
	public Statistics getStats() {
		return player.getStats();
	}

	@Override
	public void setJitterBuffer(long timeUs) {
		player.setJitterBuffer(timeUs);		
	}	


}
