package me.dmexe.telemetry.kafka;

import java.util.Map;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

@FunctionalInterface
public interface KafkaMetricsProducer {
  Map<MetricName, ? extends Metric> metrics();
}
