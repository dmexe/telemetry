package me.dmexe.telemetry.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.prometheus.client.CollectorRegistry;
import java.util.stream.IntStream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaProducerMetricsCollectorTest extends TestEnv {
  private CollectorRegistry collectorRegistry;
  private final String topic = KafkaProducerMetricsCollectorTest.class.getSimpleName();

  @BeforeEach
  void before() {
    collectorRegistry = new CollectorRegistry();
  }

  @Test
  void should_collect_metrics() {

    try (KafkaProducer<String,String> producer = newProducer()) {
      new KafkaProducerMetricsCollector(producer::metrics).register(collectorRegistry);
      IntStream.range(0, 10).forEach(n ->
        sendAndWait(producer, topic, "key", "value")
      );
    }

    await().timeout(Duration.TWO_SECONDS).untilAsserted(() -> {
      assertThat(sampleKeys(collectorRegistry))
          .containsExactly(
              "kafka_producer_metrics_batch_size_avg{test-client}",
              "kafka_producer_metrics_batch_size_max{test-client}",
              "kafka_producer_metrics_buffer_available_bytes{test-client}",
              "kafka_producer_metrics_buffer_exhausted_rate{test-client}",
              "kafka_producer_metrics_buffer_total_bytes{test-client}",
              "kafka_producer_metrics_bufferpool_wait_ratio{test-client}",
              "kafka_producer_metrics_compression_rate_avg{test-client}",
              "kafka_producer_metrics_metadata_age{test-client}",
              "kafka_producer_metrics_produce_throttle_time_avg{test-client}",
              "kafka_producer_metrics_produce_throttle_time_max{test-client}",
              "kafka_producer_metrics_record_error_rate{test-client}",
              "kafka_producer_metrics_record_queue_time_avg{test-client}",
              "kafka_producer_metrics_record_queue_time_max{test-client}",
              "kafka_producer_metrics_record_retry_rate{test-client}",
              "kafka_producer_metrics_record_send_rate{test-client}",
              "kafka_producer_metrics_record_size_avg{test-client}",
              "kafka_producer_metrics_record_size_max{test-client}",
              "kafka_producer_metrics_records_per_request_avg{test-client}",
              "kafka_producer_metrics_request_latency_avg{test-client}",
              "kafka_producer_metrics_request_latency_max{test-client}",
              "kafka_producer_metrics_requests_in_flight{test-client}",
              "kafka_producer_metrics_waiting_threads{test-client}",
              "kafka_producer_topic_metrics_byte_rate{test-client,ProducerMetricsCollectorTest}",
              "kafka_producer_topic_metrics_compression_rate{test-client,ProducerMetricsCollectorTest}",
              "kafka_producer_topic_metrics_record_error_rate{test-client,ProducerMetricsCollectorTest}",
              "kafka_producer_topic_metrics_record_retry_rate{test-client,ProducerMetricsCollectorTest}",
              "kafka_producer_topic_metrics_record_send_rate{test-client,ProducerMetricsCollectorTest}");
    });
  }
}
