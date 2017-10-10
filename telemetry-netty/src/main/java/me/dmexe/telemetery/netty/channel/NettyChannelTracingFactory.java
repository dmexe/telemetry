package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;

public interface NettyChannelTracingFactory {

  NettyChannelTracingFactory address(String address);

  NettyChannelTracingFactory address(String host, int port);

  ChannelHandler newClientHandler();

  ChannelHandler newServerHandler();

  static NettyChannelTracingFactory newFactory() {
    return new DefaultNettyChannelTracingFactory();
  }

  static String id() {
    return NettyChannelTracingContext.class.getSimpleName();
  }
}
