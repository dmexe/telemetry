package me.dmexe.telemetery.netty.channel;

interface NettyChannelTracingContext {

  void channelActive();

  void channelInactive();

  void exceptionCaught();

  void write(long bytesSize);

  void read(long bytesSize);
}