package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;

public interface ChannelTracingFactory {

  ChannelTracingFactory address(String address);

  ChannelTracingFactory address(String host, int port);

  ChannelHandler newClientHandler();

  ChannelHandler newServerHandler();

  static ChannelTracingFactory newFactory() {
    return new DefaultChannelTracingFactory();
  }

  static String id() {
    return ChannelTracingContext.class.getSimpleName();
  }
}
