package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.GaugesFactory.gauge;

import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class KafkaConsumerMetricsCollector extends Collector {
  private final KafkaMetricsProducer producer;

  public KafkaConsumerMetricsCollector(KafkaMetricsProducer producer) {
    Objects.requireNonNull(producer, "metrics producer cannot be null");
    this.producer = producer;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final List<MetricFamilySamples> samples = new LinkedList<>();

    producer.metrics().forEach((name, metric) -> {
      switch (name.group()) {
        case "consumer-metrics": {
          final String clientId = name.tags().get("client-id");
          if (clientId != null) {
            final List<String> labelValues = new ArrayList<>(1);
            labelValues.add(clientId);
            samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
          }
          break;
        }
        case "consumer-coordinator-metrics": {
          final String clientId = name.tags().get("client-id");
          if (clientId != null) {
            final List<String> labelValues = new ArrayList<>(1);
            labelValues.add(clientId);
            samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
          }
          break;
        }
        case "consumer-fetch-manager-metrics": {
          final String clientId = name.tags().get("client-id");
          if (clientId != null) {
            final List<String> labelValues = new ArrayList<>(1);
            labelValues.add(clientId);
            samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
          }
          break;
        }
        default:
          break;
      }
    });

    return samples;
  }
}
