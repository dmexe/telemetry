package me.dmexe.telemetery.netty.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.prometheus.client.CollectorRegistry;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.dmexe.telemetery.netty.channel.support.ClientChannelInit;
import me.dmexe.telemetery.netty.channel.support.ServerChannelInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class TestEnv {
  private static Duration lifecycleDuration = Duration.ofSeconds(3);
  private static Logger log = LoggerFactory.getLogger(TestEnv.class);

  Client newClient(InetSocketAddress address, CollectorRegistry collectorRegistry, Tracer tracer) throws Exception {
    final EventLoopGroup group = new NioEventLoopGroup(2);
    final BlockingQueue<HttpResponse> queue = new ArrayBlockingQueue<>(1);
    try {
      final Bootstrap bootstrap = new Bootstrap()
          .group(group)
          .channel(NioSocketChannel.class)
          .handler(new ClientChannelInit(queue, collectorRegistry, tracer));
      final ChannelFuture future = bootstrap.connect(address).sync();
      return new Client(group, future, queue);
    } catch (Exception err) {
      group.shutdownGracefully(0, lifecycleDuration.getSeconds(), TimeUnit.SECONDS);
      throw err;
    }
  }

  Server newServer(CollectorRegistry collectorRegistry, Tracer tracer) throws Exception {
    final EventLoopGroup group = new NioEventLoopGroup(2);
    try {
      final ServerBootstrap bootstrap = new ServerBootstrap()
          .group(group)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ServerChannelInit(collectorRegistry, tracer));
      final ChannelFuture future = bootstrap.bind(0).sync();
      return new Server(group, future);
    } catch (Exception err) {
      group.shutdownGracefully(0, lifecycleDuration.getSeconds(), TimeUnit.SECONDS);
      throw err;
    }
  }

  static List<String> samples(CollectorRegistry collectorRegistry, String filter) {
    return samples(collectorRegistry).stream()
        .filter(it -> it.startsWith(filter))
        .collect(Collectors.toList());
  }

  private static List<String> samples(CollectorRegistry collectorRegistry) {
    return Collections.list(collectorRegistry.metricFamilySamples()).stream()
        .flatMap(family ->
            family.samples.stream().map(sample ->
                sample.name + "{" + String.join(",", sample.labelValues) + "}=" + sample.value
            )
        )
        .sorted()
        .collect(Collectors.toList());
  }

  static class Client implements Closeable {
    private final EventLoopGroup group;
    private final ChannelFuture future;
    private final InetSocketAddress address;
    private final BlockingQueue<HttpResponse> queue;

    Client(EventLoopGroup group, ChannelFuture future, BlockingQueue<HttpResponse> queue) {
      Objects.requireNonNull(group, "group cannot be null");
      Objects.requireNonNull(future, "future cannot be null");
      Objects.requireNonNull(queue, "queue cannot be null");
      this.group = group;
      this.future = future;
      this.address = (InetSocketAddress) future.channel().localAddress();
      this.queue = queue;
      log.info("client connected at {}", address());
    }

    @Override
    public void close() {
      group.shutdownGracefully(0, lifecycleDuration.getSeconds(), TimeUnit.SECONDS);
      try {
        future.channel().closeFuture().sync();
        log.info("client disconnected at {}", address());
      } catch (InterruptedException err) {
        throw new RuntimeException(err);
      }
    }

    InetSocketAddress address() {
      return address;
    }

    Channel channel() {
      return future.channel();
    }

    HttpResponse response() {
      try {
        return queue.poll(lifecycleDuration.getSeconds(), TimeUnit.SECONDS);
      } catch (InterruptedException err) {
        throw new RuntimeException(err);
      }
    }

    void request(HttpRequest request) {
      channel().writeAndFlush(request).awaitUninterruptibly(lifecycleDuration.toMillis());
    }

    void request(HttpRequest request, Span root) {
      HttpTracingContext.addClientParentContext(channel(), root.context());
      request(request);
    }
  }

  static class Server implements Closeable {
    private final EventLoopGroup group;
    private final ChannelFuture future;
    private final InetSocketAddress address;

    Server(EventLoopGroup group, ChannelFuture future) {
      Objects.requireNonNull(group, "group cannot be null");
      Objects.requireNonNull(future, "future cannot be null");
      this.group = group;
      this.future = future;
      this.address = (InetSocketAddress) future.channel().localAddress();
      log.info("server started at {}", address());
    }

    @Override
    public void close() {
      group.shutdownGracefully(0, lifecycleDuration.getSeconds(), TimeUnit.SECONDS);
      try {
        future.channel().closeFuture().sync();
        log.info("server stopped at {}", address());
      } catch (InterruptedException err) {
        throw new RuntimeException(err);
      }
    }

    InetSocketAddress address() {
      return address;
    }
  }

}
