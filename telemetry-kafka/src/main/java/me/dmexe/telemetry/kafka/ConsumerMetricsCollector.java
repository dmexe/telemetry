package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.Gauges.gauge;

import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ConsumerMetricsCollector extends Collector {
  private final MetricsProducer producer;

  public ConsumerMetricsCollector(MetricsProducer producer) {
    Objects.requireNonNull(producer, "metrics producer cannot be null");
    this.producer = producer;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final List<MetricFamilySamples> samples = new LinkedList<>();

    producer.metrics().forEach((name, metric) -> {
      if (name.group().equals("consumer-metrics")) {
        final String clientId = name.tags().get("client-id");
        if (clientId != null) {
          final List<String> labelValues = new ArrayList<>(1);
          labelValues.add(clientId);
          samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
        }

      } else if (name.group().equals("consumer-coordinator-metrics")) {
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
