package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.Constants.CLIENT_SUBSYSTEM;
import static me.dmexe.telemetery.netty.channel.Constants.SERVER_SUBSYSTEM;

import io.netty.channel.ChannelHandler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.Objects;
import javax.annotation.Nullable;

public class DefaultHttpTracingFactory implements HttpTracingFactory {
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
    private static Counter clientHandled = handledBuilder.subsystem(CLIENT_SUBSYSTEM).register();
    private static Histogram clientLatency = latencyBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static Counter serverHandled = handledBuilder.subsystem(SERVER_SUBSYSTEM).register();
    private static Histogram serverLatency = latencyBuilder.subsystem(SERVER_SUBSYSTEM).register();
  }

  @Nullable
  private String address;

  @Nullable
  private CollectorRegistry collectorRegistry;

  @Nullable
  private Ticker ticker;

  @Nullable
  private Tracer tracer;

  @Override
  public HttpTracingFactory collectorRegistry(CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.collectorRegistry = collectorRegistry;
    return this;
  }

  @Override
  public HttpTracingFactory address(String address) {
    Objects.requireNonNull(address, "address cannot be null");
    this.address = address;
    return this;
  }

  @Override
  public HttpTracingFactory address(String host, int port) {
    Objects.requireNonNull(host, "host cannot be null");
    this.address = host + ":" + port;
    return this;
  }

  @Override
  public HttpTracingFactory ticker(Ticker ticker) {
    Objects.requireNonNull(ticker, "ticker cannot be null");
    this.ticker = ticker;
    return this;
  }

  @Override
  public HttpTracingFactory tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public ChannelHandler newClientHandler() {
    return new HttpClientTracingHandler(newClientTracingContext());
  }

  @Override
  public ChannelHandler newServerHandler() {
    return new HttpServerTracingHandler(newServerTracingContext());
  }

  private HttpTracingContext newServerTracingContext() {
    if (this.ticker == null) {
      this.ticker = System::nanoTime;
    }

    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    if (collectorRegistry == null) {
      return new DefaultHttpServerTracingContext(
          address,
          tracer,
          ticker,
          Lazy.serverHandled,
          Lazy.serverLatency);
    } else {
      return new DefaultHttpServerTracingContext(
          address,
          tracer,
          ticker,
          handledBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          latencyBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry));
    }
  }

  private HttpTracingContext newClientTracingContext() {
    if (this.ticker == null) {
      this.ticker = System::nanoTime;
    }

    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    if (collectorRegistry == null) {
      return new DefaultHttpClientTracingContext(
          address,
          tracer,
          ticker,
          Lazy.clientHandled,
          Lazy.clientLatency);
    } else {
      return new DefaultHttpClientTracingContext(
          address,
          tracer,
          ticker,
          handledBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          latencyBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry));
    }
  }
}
