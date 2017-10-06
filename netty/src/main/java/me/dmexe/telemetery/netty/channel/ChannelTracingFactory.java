package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;
import io.prometheus.client.CollectorRegistry;
import java.util.function.Supplier;

public interface ChannelTracingFactory {
  ChannelTracingFactory address(String address);
  ChannelTracingFactory address(String host, int port);
  ChannelTracingFactory collectorRegistry(CollectorRegistry collectorRegistry);
  ChannelTracingFactory ticker(Ticker ticker);

  ChannelHandler newClientHandler();
  ChannelHandler newServerHandler();

  static ChannelTracingFactory newFactory() {
    return new DefaultChannelTracingFactory();
  }

  static String id() {
    return ChannelTracingContext.class.getSimpleName();
  }
}
