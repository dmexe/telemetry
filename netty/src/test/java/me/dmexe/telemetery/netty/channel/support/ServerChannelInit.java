package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import me.dmexe.telemetery.netty.channel.ChannelTracingFactory;

public class ServerChannelInit extends ChannelInitializer<SocketChannel> {
  private final CollectorRegistry collectorRegistry;

  public ServerChannelInit(CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.collectorRegistry = collectorRegistry;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    final ChannelPipeline pipe = ch.pipeline();
    pipe.addLast(
        ChannelTracingFactory.id(),
        ChannelTracingFactory.newFactory()
            .address(":0")
            .ticker(new ConstantTicker())
            .collectorRegistry(collectorRegistry)
            .newServerHandler());
    pipe.addLast("http-decoder", new HttpRequestDecoder());
    pipe.addLast("http-aggregator", new HttpObjectAggregator(65536));
    pipe.addLast("http-encoder", new HttpResponseEncoder());
    pipe.addLast("handler", new ServerHandler());
  }
}
