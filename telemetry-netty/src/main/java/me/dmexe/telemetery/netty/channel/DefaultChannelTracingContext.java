package me.dmexe.telemetery.netty.channel;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleTimer;
import io.prometheus.client.Summary;
import java.util.Objects;

class DefaultChannelTracingContext implements ChannelTracingContext {
  private static final long NULL_NANO = -1L;
  private static final String ACTIVE = "active";
  private static final String INACTIVE = "inactive";
  private static final String FAILED = "failed";

  private final String address;
  private final Ticker ticker;
  private final Gauge connections;
  private final Counter connects;
  private final Summary duration;
  private final Counter bytesSend;
  private final Counter bytesReceived;
  private long connectionStartTimeNanos;

  DefaultChannelTracingContext(
      String address,
      Ticker ticker,
      Gauge connections,
      Counter connects,
      Summary duration,
      Counter bytesSend,
      Counter bytesReceived) {
    Objects.requireNonNull(address, "address cannot be null");
    Objects.requireNonNull(ticker, "ticker cannot be null");
    Objects.requireNonNull(connections, "connections cannot be null");
    Objects.requireNonNull(connects, "connects cannot be null");
    Objects.requireNonNull(duration, "duration cannot be null");
    Objects.requireNonNull(bytesSend, "bytesSend cannot be null");
    Objects.requireNonNull(bytesReceived, "bytesReceived cannot be null");

    this.address = address;
    this.ticker = ticker;
    this.connections = connections;
    this.connects = connects;
    this.duration = duration;
    this.bytesSend = bytesSend;
    this.bytesReceived = bytesReceived;
    this.connectionStartTimeNanos = NULL_NANO;
  }

  @Override
  public void channelActive() {
    connectionStartTimeNanos = ticker.nanoTime();
    connections.labels(address).inc();
    connects.labels(address, ACTIVE).inc();
  }

  @Override
  public void channelInactive() {
    connections.labels(address).dec();
    connects.labels(address, INACTIVE).inc();

    if (connectionStartTimeNanos != NULL_NANO) {
      final double elapsed = SimpleTimer.elapsedSecondsFromNanos(
          connectionStartTimeNanos,
          ticker.nanoTime());
      duration.labels(address).observe(elapsed);
      connectionStartTimeNanos = NULL_NANO;
    }
  }

  @Override
  public void exceptionCaught() {
    connects.labels(address, FAILED).inc();
  }

  @Override
  public void write(long bytesSize) {
    bytesSend.labels(address).inc(bytesSize);
  }

  @Override
  public void read(long bytesSize) {
    bytesReceived.labels(address).inc(bytesSize);
  }
}
