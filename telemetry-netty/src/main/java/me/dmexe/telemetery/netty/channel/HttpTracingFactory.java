package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;
import io.opentracing.Tracer;

public interface HttpTracingFactory {
  HttpTracingFactory address(String address);

  HttpTracingFactory address(String host, int port);

  HttpTracingFactory tracer(Tracer tracer);

  ChannelHandler newClientHandler();

  ChannelHandler newServerHandler();

  static HttpTracingFactory newFactory() {
    return new DefaultHttpTracingFactory();
  }

  static String id() {
    return HttpTracingContext.class.getSimpleName();
  }
}