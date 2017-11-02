package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;
import io.opentracing.Tracer;

public interface NettyHttpTracingFactory {
  NettyHttpTracingFactory address(String address);

  NettyHttpTracingFactory address(String host, int port);

  NettyHttpTracingFactory tracer(Tracer tracer);

  NettyHttpTracingFactory pathsStartWith(String... path);

  ChannelHandler newClientHandler();

  ChannelHandler newServerHandler();

  static NettyHttpTracingFactory newFactory() {
    return new DefaultNettyHttpTracingFactory();
  }

  static String id() {
    return NettyHttpTracingContext.class.getSimpleName();
  }
}