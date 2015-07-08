package com.optman.rtp.receiver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.optman.rtp.receiver.RtpSink;
import com.optman.rtp.player.Statistics;

import android.util.Log;

public abstract class RtpStream implements RtpSink{

	protected long timestamp;

	private long maxTimestamp = 0;
	private long tsWrapAroundCount = 0;
		
	private long maxSeqNum = 0;
	private long snWrapAroundCount = 0;
		
	private long lastSeqNum;
	protected long currentSeqNum;
		
	protected Statistics stats;	

	public RtpStream(Statistics stats){
		this.stats = stats;
	}	
	
	public void close(){
		
	}
	
	public void onRtp(byte[] data, int dataSize){
		try {
			decodeRtp(data, dataSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	
	private void decodeRtp(byte[] buf, int length) throws IOException{
		
		DataInputStream reader = new DataInputStream(new ByteArrayInputStream(buf, 0, length));
		
		byte b = reader.readByte();
		
//		int version = (b & 0xc0) >> 6;
//		boolean padding = (b & 0x20) != 0;
//		boolean extension = (b & 0x10) != 0;
		int csrcCount = b & 0x0f;		

		b = reader.readByte();
		
		//boolean marker = (b & 0x80 ) != 0;
//		int payloadType = b & 0x7F;
		
		long sequenceNumber = reader.readShort() & 0x000000000000ffffL;
		long timestamp = reader.readInt() & 0x00000000ffffffffL;
		/*int ssrc = */reader.readInt();
		
		for(int i = 0; i < csrcCount; i++)
			reader.readInt();

	
//		Log.i("RTP", "v:" + version + " p:" + padding + " e:" + extension + " p:" + payloadType +
//				" sn:" + sequenceNumber + " ts:" + timestamp);

		
		this.currentSeqNum = extendSequenceNumber(sequenceNumber);		
		this.timestamp = extendTimestamp(timestamp);


		//duplicated packet.
		if(currentSeqNum == lastSeqNum){
			Log.i("RTP", "duplicate detected!");
			return;
		}
	
		if(discontinue() && lastSeqNum != 0){
			Log.i("RTP", "discontinue detected! " + (currentSeqNum - lastSeqNum));
					
			
		if(this.stats != null && currentSeqNum > lastSeqNum)
			this.stats.transportDropPacketCount += currentSeqNum - lastSeqNum;
		}
	
		decodePayload(reader);
			
		lastSeqNum = currentSeqNum;
	}
	
	protected boolean discontinue(){
		return lastSeqNum + 1 != currentSeqNum;
	}
	
	private long extendTimestamp(long timestamp){

		final long maxDelta = 90000*10;		//90K clock rate in 10 seconds. 
		
		long udelta = (timestamp - maxTimestamp) & 0x00000000ffffffffL;

		long wrapAroundCount;

		if(udelta < maxDelta){
			if(timestamp < maxTimestamp)
				tsWrapAroundCount++;
			
			maxTimestamp = timestamp;

			wrapAroundCount = tsWrapAroundCount;

		}else if(tsWrapAroundCount > 0){
			//a delay from last round.
			wrapAroundCount = tsWrapAroundCount - 1;
		}else{
			//first round
			maxTimestamp = timestamp;			
			wrapAroundCount = 0;
		}		
		
		
		return timestamp + 0xFFFFFFFF*wrapAroundCount;			

	}
	
	private long extendSequenceNumber(long seq){
		
		final long maxDropout = 3000;
		//final long maxMisorder = 100;
				
		long udelta = (seq - maxSeqNum) & 0x000000000000ffffL;
	
		long wrapAroundCount;
		

		if(udelta < maxDropout){
			if(seq < maxSeqNum){
				snWrapAroundCount++;
				Log.i("RTP", "sequence wrap around.");
			}
			
			maxSeqNum = seq;

			wrapAroundCount = snWrapAroundCount;
			
		}else if(snWrapAroundCount > 0){
			//Disorder, a delay from last round.
			wrapAroundCount = (snWrapAroundCount - 1);			
	
		}else{
			//first round
			maxSeqNum = seq;
			wrapAroundCount = 0;			
		}

		return seq + 0xFFFF*wrapAroundCount;
	}
	
	
	protected abstract boolean decodePayload(DataInputStream reader) throws IOException;
		
	
}
