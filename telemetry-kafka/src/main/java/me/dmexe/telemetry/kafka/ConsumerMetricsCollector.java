package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.Gauges.gauge;

import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

public class ConsumerMetricsCollector extends Collector {
  private final Supplier<Map<MetricName, ? extends Metric>> metrics;

  public ConsumerMetricsCollector(Supplier<Map<MetricName, ? extends Metric>> metrics) {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    this.metrics = metrics;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final List<MetricFamilySamples> samples = new LinkedList<>();

    metrics.get().forEach((name, metric) -> {
      if (name.group().equals("consumer-metric")) {
        final String clientId = name.tags().get("client-id");
        if (clientId != null) {
          final List<String> labelValues = new ArrayList<>(1);
          labelValues.add(clientId);
          samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
        }
      } else if (name.group().equals("consumer-coordinator-metric")) {
        final String clientId = name.tags().get("client-id");
        if (clientId != null) {
          final List<String> labelValues = new ArrayList<>(1);
          labelValues.add(clientId);
          samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
        }
      }
    });

    return samples;
  }
}