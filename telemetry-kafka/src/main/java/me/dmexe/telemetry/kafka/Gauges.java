package me.dmexe.telemetry.kafka;

import io.prometheus.client.GaugeMetricFamily;
import java.util.Arrays;
import org.apache.kafka.common.MetricName;

class Gauges {
  private static String safe(String value) {
    return value.replace("-", "_");
  }

  static GaugeMetricFamily gauge(MetricName metricName, String... tags) {
    final String name = "kafka_" + safe(metricName.group()) + "_" + safe(metricName.name());
    return new GaugeMetricFamily(
        name,
        metricName.description(),
        Arrays.asList(tags));
  }
}
