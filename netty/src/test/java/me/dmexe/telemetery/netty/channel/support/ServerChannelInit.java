package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.opentracing.Tracer;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import me.dmexe.telemetery.netty.channel.ChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.HttpTracingFactory;

public class ServerChannelInit extends ChannelInitializer<SocketChannel> {
  private final CollectorRegistry collectorRegistry;
  private final Tracer tracer;

  public ServerChannelInit(CollectorRegistry collectorRegistry, Tracer tracer) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.collectorRegistry = collectorRegistry;
    this.tracer = tracer;
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
    pipe.addLast(
        HttpTracingFactory.id(),
        HttpTracingFactory.newFactory()
            .address(":0")
            .ticker(new ConstantTicker())
            .tracer(tracer)
            .collectorRegistry(collectorRegistry)
            .newServerHandler());
    pipe.addLast("handler", new ServerHandler());
  }
}
