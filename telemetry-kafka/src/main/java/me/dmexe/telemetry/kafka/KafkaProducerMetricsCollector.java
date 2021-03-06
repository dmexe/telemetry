package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.GaugesFactory.gauge;

import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class KafkaProducerMetricsCollector extends Collector {
  private final KafkaMetricsProducer producer;

  public KafkaProducerMetricsCollector(KafkaMetricsProducer producer) {
    Objects.requireNonNull(producer, "metrics producer cannot be null");
    this.producer = producer;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final List<MetricFamilySamples> samples = new LinkedList<>();

    producer.metrics().forEach((name, metric) -> {
      if (name.group().equals("producer-metrics")) {
        final String clientId = name.tags().get("client-id");

        if (clientId != null) {
          final List<String> labelValues = new ArrayList<>(1);
          labelValues.add(clientId);

          samples.add(gauge(name, "client_id").addMetric(labelValues, metric.value()));
        }
      } else if (name.group().equals("producer-topic-metrics")) {
        final String clientId = name.tags().get("client-id");
        final String topic = name.tags().get("topic");

        if (clientId != null && topic != null) {
          final List<String> labelValues = new ArrayList<>(2);
          labelValues.add(clientId);
          labelValues.add(topic);

          samples.add(gauge(name, "client_id", "topic").addMetric(labelValues, metric.value()));
        }
      }
    });

    return samples;
  }
}
