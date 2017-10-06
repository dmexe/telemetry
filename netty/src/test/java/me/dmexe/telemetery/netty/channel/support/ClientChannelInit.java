package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.logging.LoggingHandler;
import io.opentracing.Tracer;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import java.util.Queue;
import me.dmexe.telemetery.netty.channel.ChannelTracingFactory;
import me.dmexe.telemetery.netty.channel.HttpTracingFactory;

public class ClientChannelInit extends ChannelInitializer<SocketChannel> {
  private final Queue<HttpResponse> queue;
  private final CollectorRegistry collectorRegistry;
  private final Tracer tracer;

  public ClientChannelInit(
      Queue<HttpResponse> queue,
      CollectorRegistry collectorRegistry,
      Tracer tracer) {
    Objects.requireNonNull(queue, "queue cannot be null");
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.queue = queue;
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
            .newClientHandler());
    pipe.addLast("log", new LoggingHandler());
    pipe.addLast(new HttpClientCodec());
    pipe.addLast(
        HttpTracingFactory.id(),
        HttpTracingFactory.newFactory()
            .address(":0")
            .ticker(new ConstantTicker())
            .collectorRegistry(collectorRegistry)
            .tracer(tracer)
            .newClientHandler());
    pipe.addLast(new ClientHandler(queue));
  }
}
