package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.NettyConstants.CLIENT_SUBSYSTEM;
import static me.dmexe.telemetery.netty.channel.NettyConstants.SERVER_SUBSYSTEM;

import io.netty.channel.ChannelHandler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class DefaultNettyHttpTracingFactory implements NettyHttpTracingFactory {
  private static final Counter.Builder handledBuilder = Counter.build()
      .namespace("http")
      .name("handled_total")
      .labelNames("address", "http_code", "http_method")
      .help("Total number of completed HTTP requests, regardless of success or failure.");

  private static final Histogram.Builder latencyBuilder = Histogram.build()
      .namespace("http")
      .name("handled_latency_seconds")
      .labelNames("address", "http_code", "http_method")
      .help("Histogram of response latency (seconds) of HTTP that had been application-level "
          + "handled.");

  private static class Lazy {
    private static final Counter clientHandled =
        handledBuilder.subsystem(CLIENT_SUBSYSTEM).register();
    private static final Histogram clientLatency =
        latencyBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Counter serverHandled =
        handledBuilder.subsystem(SERVER_SUBSYSTEM).register();
    private static final Histogram serverLatency =
        latencyBuilder.subsystem(SERVER_SUBSYSTEM).register();
  }

  private String address;
  private Ticker ticker;

  @Nullable
  private CollectorRegistry collectorRegistry;

  @Nullable
  private Tracer tracer;


  public DefaultNettyHttpTracingFactory() {
    this.ticker = System::nanoTime;
  }

  /**
   * Assign a {@link CollectorRegistry}, it's only for testing.
   *
   * @param collectorRegistry override default collector registry.
   * @return the factory.
   */
  public DefaultNettyHttpTracingFactory collectorRegistry(CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.collectorRegistry = collectorRegistry;
    this.address = ":0";
    return this;
  }

  /**
   * Assign a {@link Ticker}, it's only for testing.
   *
   * @param ticker override ticker.
   * @return the factory.
   */
  public DefaultNettyHttpTracingFactory ticker(Ticker ticker) {
    Objects.requireNonNull(ticker, "ticker cannot be null");
    this.ticker = ticker;
    return this;
  }

  @Override
  public NettyHttpTracingFactory address(String address) {
    Objects.requireNonNull(address, "address cannot be null");
    this.address = address;
    return this;
  }

  @Override
  public NettyHttpTracingFactory address(String host, int port) {
    Objects.requireNonNull(host, "host cannot be null");
    this.address = host + ":" + port;
    return this;
  }

  @Override
  public NettyHttpTracingFactory tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public ChannelHandler newClientHandler() {
    return new NettyHttpClientTracingHandler(newClientTracingContext());
  }

  @Override
  public ChannelHandler newServerHandler() {
    return new NettyHttpServerTracingHandler(newServerTracingContext());
  }

  private NettyHttpTracingContext newServerTracingContext() {
    final Ticker ticker = this.ticker == null ? System::nanoTime : this.ticker;
    final Tracer tracer = this.tracer == null ? GlobalTracer.get() : this.tracer;

    if (collectorRegistry == null) {
      return new DefaultNettyHttpServerTracingContext(
          address,
          tracer,
          ticker,
          Lazy.serverHandled,
          Lazy.serverLatency);
    } else {
      return new DefaultNettyHttpServerTracingContext(
          address,
          tracer,
          ticker,
          handledBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          latencyBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry));
    }
  }

  private NettyHttpTracingContext newClientTracingContext() {
    final Ticker ticker = this.ticker == null ? System::nanoTime : this.ticker;
    final Tracer tracer = this.tracer == null ? GlobalTracer.get() : this.tracer;

    if (collectorRegistry == null) {
      return new DefaultNettyHttpClientTracingContext(
          address,
          tracer,
          ticker,
          Lazy.clientHandled,
          Lazy.clientLatency);
    } else {
      return new DefaultNettyHttpClientTracingContext(
          address,
          tracer,
          ticker,
          handledBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          latencyBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry));
    }
  }
}
