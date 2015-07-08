package com.optman.rtp.receiver;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.optman.librtsp.BuildConfig;
import com.optman.rtp.receiver.RtpStream;
import com.optman.rtp.receiver.Sample;
import com.optman.rtp.receiver.SampleHandler;
import com.optman.rtp.player.Statistics;

import android.util.Log;

public class RtpAvcStream extends RtpStream{


	private boolean sync;
	private ByteArrayOutputStream fragmentBuf;
	private int fuaType;
	
	private SampleHandler handler;
	
	private boolean isSync = false;
	private boolean enforceSync = false;  
	
	public RtpAvcStream(SampleHandler handler, Statistics stats){
		super(stats);
		this.handler = handler;
	}	

	public boolean decodePayload(DataInputStream reader) throws IOException{
		
		if(isSync && discontinue())
			isSync = false;
		
		int naluHeader= reader.readByte() & 0xFF;
		
		//int forbidden_zero_bit = (naluHeader >>> 7) & 0x1;
		//int nal_ref_idc = (naluHeader >>> 5) & 0x03;
		int type = naluHeader & 0x1F;
		
		//Log.i("NAL", "F:" + forbidden_zero_bit + " NRI:" + nal_ref_idc + " Type:" + type);
		
		boolean result =false;
		
		if(type >= 1 && type <= 23){
			result = decodeSingleNalu(type, reader, (byte)naluHeader);
		}/*else if(type == 24){
			decodeStapA(reader);
		}else if(type == 25){
			decodeStapB(reader);
		}else if(type == 26){
			decodeMtap16(reader);
		}else if(type == 27){
			decodeMtap24(reader);
		}*/else if(type == 28){
			result = decodeFuA(reader, (byte)naluHeader);
		}/*else if(type == 29){
			decodeFuB(reader);
		}*/ else if(type == 30 || type == 31){
			//ignore.
		}		
		else{
			Log.e("NAL", "unknown type!!!!!" + type);
			if(BuildConfig.DEBUG)
				throw new RuntimeException();
		}
		
		return result;
	}
	
	private void onNalu(Sample nalu){

		if(stats != null){
			stats.videoFrameCount++;
			if(nalu.keyframe)
				stats.syncframeCount++;
		}			
		
		if(nalu.keyframe)
			isSync = true;
		
		if(enforceSync && !isSync){
			Log.i("NAL", "drop unsync nalu.");
			return;
		}
				
		nalu.isVideo = true;
		if(handler != null){
			nalu.data = addStartcode(nalu.data);
			handler.onSample(nalu);
		}
	}
	
	private byte[] addStartcode(byte[] src){
		byte[] dst = new byte[src.length + 4];
		dst[0] = 0;
		dst[1] = 0;
		dst[2] = 0;
		dst[3] = 1;
		System.arraycopy(src, 0, dst, 4, src.length);
		
		return dst;		
	}

	private boolean decodeSingleNalu(int type, DataInputStream reader, byte header) throws IOException {
		
		Sample nalu = new Sample();
		nalu.timestampUs = timestamp*1000000/90000;
		nalu.keyframe = (type == 5);
		
		nalu.data = new byte[reader.available() + 1];
		nalu.data[0] = header;
		reader.readFully(nalu.data, 1, reader.available());
		
		onNalu(nalu);
		
		return true;
	}

//	private void decodeStapA(DataInputStream reader) {
//		if(BuildConfig.DEBUG)
//			throw new RuntimeException();
//		
//	}
//
//	private void decodeStapB(DataInputStream reader) {
//		if(BuildConfig.DEBUG)
//			throw new RuntimeException();		
//	}
//
//	private void decodeMtap16(DataInputStream reader) {
//		if(BuildConfig.DEBUG)
//			throw new RuntimeException();
//	}
//
//	private void decodeMtap24(DataInputStream reader) {
//		if(BuildConfig.DEBUG)
//			throw new RuntimeException();
//	}

	private boolean decodeFuA(DataInputStream reader, byte header) throws IOException {
		int fuHeader = reader.readByte() & 0xff;
	
		int startBit = (fuHeader >> 7) & 0x01;
		int endBit = (fuHeader >> 6) & 0x01;
		int reservedBit = (fuHeader >> 5) & 0x01;
		if(reservedBit != 0)
			throw new IOException();
		
		int naluType = fuHeader & 0x1f;
		
		//Log.i("FUA", "FUA S:" + startBit + " E:" + endBit + " T:" + naluType + " SN:" + currentSeqNum);
		
		if(discontinue() && sync){
			Log.i("FUA", "droped!");
			sync = false;
			return false;
		}		
		
		if(startBit == 1){
			 fuaType = naluType; 
			 fragmentBuf = new ByteArrayOutputStream();
	 		 int naluHeader = (header | 0x1f) & (0xe0 | naluType);
			 fragmentBuf.write(naluHeader);
			 sync = true;
		}
		
		if(!sync)
			return false;

		byte[] data = new byte[reader.available()];
		reader.readFully(data);			
		fragmentBuf.write(data);	
			

		if(endBit == 1){
			Sample nalu = new Sample();
			nalu.timestampUs = timestamp*1000000/90000;
			nalu.keyframe = (fuaType == 5);
			nalu.data = fragmentBuf.toByteArray();

			onNalu(nalu);
			
			sync = false;
		}			
		
		return true;
	}

//	private void decodeFuB(DataInputStream reader) {
//		if(BuildConfig.DEBUG)
//			throw new RuntimeException();
//	}
	
}
