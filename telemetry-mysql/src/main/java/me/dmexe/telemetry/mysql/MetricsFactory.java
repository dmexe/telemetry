package me.dmexe.telemetry.mysql;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

class MetricsFactory {
  private static final Counter.Builder totalBuilder = Counter.build()
      .namespace("mysql")
      .name("started_total")
      .labelNames("name", "database")
      .help("A total number of database queries were been started.");

  private static final Histogram.Builder latencyBuilder = Histogram.build()
      .namespace("mysql")
      .name("latency_seconds")
      .labelNames("name", "database")
      .help("A seconds which spend to execute database queries.");

  static final MetricsFactory DEFAULT;

  static  {
    DEFAULT = new MetricsFactory(CollectorRegistry.defaultRegistry);
  }

  private final Counter total;
  private final Histogram latency;

  private MetricsFactory(CollectorRegistry collectorRegistry) {
    this.total = totalBuilder.register(collectorRegistry);
    this.latency = latencyBuilder.register(collectorRegistry);
  }

  public Counter.Child getTotal(String name, String database) {
    return total.labels(name, database);
  }

  public Histogram.Child getLatency(String name, String database) {
    return latency.labels(name, database);
  }
}
