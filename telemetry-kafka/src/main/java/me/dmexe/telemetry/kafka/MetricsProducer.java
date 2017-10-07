package me.dmexe.telemetry.kafka;

import java.util.Map;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

@FunctionalInterface
interface MetricsProducer {
  Map<MetricName, ? extends Metric> metrics();
}
