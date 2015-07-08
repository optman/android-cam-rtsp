package com.optman.RtspServer;

import java.net.InetAddress;

import com.optman.rtp.sender.RtpSocket;

public class RtpSession {
	InetAddress host;
	int port;
	RtpSocket tcpSocket;	
}
