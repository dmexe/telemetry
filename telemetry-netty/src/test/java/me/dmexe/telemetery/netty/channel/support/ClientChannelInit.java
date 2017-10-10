package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Tracer;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import java.util.Queue;
import me.dmexe.telemetery.netty.channel.NettyChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.DefaultNettyChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.DefaultNettyHttpTracingFactory;
import me.dmexe.telemetery.netty.channel.NettyHttpTracingFactory;

public class ClientChannelInit extends ChannelInitializer<SocketChannel> {
  private final Queue<HttpResponse> queue;

  private final NettyChannelTracingFactory channelTracingFactory;
  private final NettyHttpTracingFactory httpTracingFactory;

  public ClientChannelInit(
      Queue<HttpResponse> queue,
      CollectorRegistry collectorRegistry,
      Tracer tracer) {
    Objects.requireNonNull(queue, "queue cannot be null");
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");

    this.queue = queue;

    this.channelTracingFactory = new DefaultNettyChannelTracingFactory()
        .ticker(new ConstantTicker())
        .collectorRegistry(collectorRegistry)
        .address(":0");

    this.httpTracingFactory = new DefaultNettyHttpTracingFactory()
        .ticker(new ConstantTicker())
        .collectorRegistry(collectorRegistry)
        .tracer(tracer)
        .address(":0");
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    final ChannelPipeline pipe = ch.pipeline();
    pipe.addLast(NettyChannelTracingFactory.id(), channelTracingFactory.newClientHandler());
    pipe.addLast(new HttpClientCodec());
    pipe.addLast(NettyHttpTracingFactory.id(), httpTracingFactory.newClientHandler());
    pipe.addLast(new ClientHandler(queue));
  }
}
