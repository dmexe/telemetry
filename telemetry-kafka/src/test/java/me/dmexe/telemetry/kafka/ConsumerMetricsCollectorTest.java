package me.dmexe.telemetry.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import io.prometheus.client.CollectorRegistry;
import java.time.Duration;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumerMetricsCollectorTest extends TestEnv {
  private CollectorRegistry collectorRegistry;
  private final String topic = ConsumerMetricsCollectorTest.class.getSimpleName();
  private final Duration subscribeTimeout = Duration.ofSeconds(30);

  @BeforeEach
  void before() {
    collectorRegistry = new CollectorRegistry();
  }

  @Test
  void should_collect_metrics() throws Exception {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {

      new ConsumerMetricsCollector(consumer::metrics).register(collectorRegistry);
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> {
        sendAndWait(producer, topic, "key", "value");
      });

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();
      });

      consumer.commitSync();
    }

    assertThat(sampleKeys(collectorRegistry))
        .isNotEmpty()
        .areAtLeast(13, new Condition<>(s -> true, "true"));
  }
}
