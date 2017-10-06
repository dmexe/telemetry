package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpResponse;
import io.prometheus.client.CollectorRegistry;
import java.util.Objects;
import java.util.Queue;
import me.dmexe.telemetery.netty.channel.ChannelTracingFactory;

public class ClientChannelInit extends ChannelInitializer<SocketChannel> {
  private final Queue<HttpResponse> queue;
  private final CollectorRegistry collectorRegistry;

  public ClientChannelInit(Queue<HttpResponse> queue, CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(queue, "queue cannot be null");
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.queue = queue;
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
            .newClientHandler());
    pipe.addLast(new HttpClientCodec());
    pipe.addLast(new ClientHandler(queue));
  }
}
