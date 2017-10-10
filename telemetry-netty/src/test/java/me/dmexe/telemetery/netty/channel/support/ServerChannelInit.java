package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.opentracing.Tracer;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import me.dmexe.telemetery.netty.channel.NettyChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.DefaultNettyChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.DefaultNettyHttpTracingFactory;
import me.dmexe.telemetery.netty.channel.NettyHttpTracingFactory;

public class ServerChannelInit extends ChannelInitializer<SocketChannel> {
  private final NettyHttpTracingFactory httpTracingFactory;
  private final NettyChannelTracingFactory channelTracingFactory;

  public ServerChannelInit(CollectorRegistry collectorRegistry, Tracer tracer) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");

    this.channelTracingFactory = new DefaultNettyChannelTracingFactory()
        .collectorRegistry(collectorRegistry)
        .ticker(new ConstantTicker())
        .address(":0");

    this.httpTracingFactory = new DefaultNettyHttpTracingFactory()
        .collectorRegistry(collectorRegistry)
        .ticker(new ConstantTicker())
        .tracer(tracer)
        .address(":0");
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    final ChannelPipeline pipe = ch.pipeline();
    pipe.addLast(NettyChannelTracingFactory.id(), channelTracingFactory.newServerHandler());
    pipe.addLast("http-decoder", new HttpRequestDecoder());
    pipe.addLast("http-aggregator", new HttpObjectAggregator(65536));
    pipe.addLast("http-encoder", new HttpResponseEncoder());
    pipe.addLast(NettyHttpTracingFactory.id(), httpTracingFactory.newServerHandler());
    pipe.addLast("handler", new ServerHandler());
  }
}
