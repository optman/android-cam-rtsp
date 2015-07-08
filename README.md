LiveView -- udp -- > RtspServer -- udp/tcp -> MyRtsp

Camera/AudioRecord -> MediaCodec -> H264/AAC -> RTP

MyRtsp(RtspPlayer) is build on ExoPlayer.

Due to pixel format, now the only known supported device is Nexus 5! 








