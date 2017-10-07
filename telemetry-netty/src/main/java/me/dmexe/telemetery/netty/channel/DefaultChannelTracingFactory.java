package me.dmexe.telemetery.netty.channel;

import io.netty.channel.ChannelHandler;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class DefaultChannelTracingFactory implements ChannelTracingFactory {

  private static final String CLIENT_SUBSYSTEM = "client";
  private static final String SERVER_SUBSYSTEM = "server";

  private static final Gauge.Builder connectionsBuilder = Gauge.build()
      .namespace("netty")
      .name("connections_count")
      .labelNames("address")
      .help("A gauge of the total number of connections that are currently open in the channel.");

  private static final Counter.Builder connectsBuilder = Counter.build()
      .namespace("netty")
      .name("connects_total")
      .labelNames("address", "status")
      .help("A counter of the total number of connections made.");

  private static final Summary.Builder durationBuilder = Summary.build()
      .namespace("netty")
      .subsystem("client")
      .name("connection_duration_seconds")
      .labelNames("address")
      .help("A summary of the duration of the lifetime of a connection.");

  private static final Counter.Builder bytesSendBuilder = Counter.build()
      .namespace("netty")
      .name("send_bytes")
      .labelNames("address")
      .help("A counter of the total number of sent bytes.");

  private static final Counter.Builder bytesReceivedBuilder = Counter.build()
      .namespace("netty")
      .name("received_bytes")
      .labelNames("address")
      .help("A counter of the total number of received bytes.");

  private static class Lazy {

    private static final Gauge clientConnections =
        connectionsBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Counter clientConnects =
        connectsBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Summary clientDuration =
        durationBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Counter clientBytesSend =
        bytesSendBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Counter clientBytesReceived =
        bytesReceivedBuilder.subsystem(CLIENT_SUBSYSTEM).register();

    private static final Gauge serverConnections =
        connectionsBuilder.subsystem(SERVER_SUBSYSTEM).register();

    private static final Counter serverConnects =
        connectsBuilder.subsystem(SERVER_SUBSYSTEM).register();

    private static final Summary serverDuration =
        durationBuilder.subsystem(SERVER_SUBSYSTEM).register();

    private static final Counter serverBytesSend =
        bytesSendBuilder.subsystem(SERVER_SUBSYSTEM).register();

    private static final Counter serverBytesReceived =
        bytesReceivedBuilder.subsystem(SERVER_SUBSYSTEM).register();
  }

  private Ticker ticker;
  private String address;

  @Nullable
  private CollectorRegistry collectorRegistry;

  public DefaultChannelTracingFactory() {
    this.ticker = System::nanoTime;
  }

  /**
   * Assign a {@link CollectorRegistry}, it's only for testing.
   */
  public DefaultChannelTracingFactory collectorRegistry(CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.collectorRegistry = collectorRegistry;
    this.address = ":0";
    return this;
  }

  /**
   * Assign a {@link Ticker}, it's only for testing.
   */
  public DefaultChannelTracingFactory ticker(Ticker ticker) {
    Objects.requireNonNull(ticker, "ticker cannot be null");
    this.ticker = ticker;
    return this;
  }

  @Override
  public ChannelTracingFactory address(String address) {
    Objects.requireNonNull(address, "address cannot be null");
    this.address = address;
    return this;
  }

  @Override
  public ChannelTracingFactory address(String host, int port) {
    Objects.requireNonNull(host, "host cannot be null");
    this.address = host + ":" + port;
    return this;
  }

  @Override
  public ChannelHandler newClientHandler() {
    return new ChannelTracingHandler(newClientTracingContext());
  }

  @Override
  public ChannelHandler newServerHandler() {
    return new ChannelTracingHandler(newServerTracingContext());
  }

  private ChannelTracingContext newClientTracingContext() {
    final Ticker ticker = this.ticker == null ? System::nanoTime : this.ticker;

    if (collectorRegistry == null) {
      return new DefaultChannelTracingContext(
          address,
          ticker,
          Lazy.clientConnections,
          Lazy.clientConnects,
          Lazy.clientDuration,
          Lazy.clientBytesSend,
          Lazy.clientBytesReceived);
    } else {
      return new DefaultChannelTracingContext(
          address,
          ticker,
          connectionsBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          connectsBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          durationBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          bytesSendBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry),
          bytesReceivedBuilder.subsystem(CLIENT_SUBSYSTEM).register(collectorRegistry));
    }
  }

  private ChannelTracingContext newServerTracingContext() {
    final Ticker ticker = this.ticker == null ? System::nanoTime : this.ticker;

    if (collectorRegistry == null) {
      return new DefaultChannelTracingContext(
          address,
          ticker,
          Lazy.serverConnections,
          Lazy.serverConnects,
          Lazy.serverDuration,
          Lazy.serverBytesSend,
          Lazy.serverBytesReceived);
    } else {
      return new DefaultChannelTracingContext(
          address,
          ticker,
          connectionsBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          connectsBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          durationBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          bytesSendBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry),
          bytesReceivedBuilder.subsystem(SERVER_SUBSYSTEM).register(collectorRegistry));
    }
  }
}