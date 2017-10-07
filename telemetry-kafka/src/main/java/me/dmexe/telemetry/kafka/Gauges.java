package me.dmexe.telemetry.kafka;

import io.prometheus.client.GaugeMetricFamily;
import java.util.Arrays;
import org.apache.kafka.common.MetricName;

class Gauges {
  private static String safe(String value) {
    return value.replace("-", "_");
  }

  static GaugeMetricFamily gauge(MetricName metricName, String... tags) {
    return new GaugeMetricFamily(
        safe("kafka_" + metricName.group() + "_" + metricName.name()),
        metricName.description(),
        Arrays.asList(tags));
  }
}
