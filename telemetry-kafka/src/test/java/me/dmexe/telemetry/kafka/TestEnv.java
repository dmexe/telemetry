package me.dmexe.telemetry.kafka;

import io.opentracing.mock.MockSpan;
import io.prometheus.client.CollectorRegistry;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

abstract class TestEnv {
  Duration lifecycleDuration = Duration.ofSeconds(3);

  private String bootstrapServer() {
    return System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092");
  }

  List<String> sampleKeys(CollectorRegistry collectorRegistry) {
    return Collections.list(collectorRegistry.metricFamilySamples()).stream()
        .map(sample -> {
          final List<String> labels = sample.samples.stream()
              .flatMap(s -> s.labelValues.stream())
              .collect(Collectors.toList());
          return sample.name + "{" + String.join(",",  labels) + "}";
        })
        .sorted()
        .collect(Collectors.toList());
  }

  KafkaProducer<String,String> newProducer() {
    final Properties props = new Properties();
    props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer());
    props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.setProperty(ProducerConfig.ACKS_CONFIG, "all");
    props.setProperty(ProducerConfig.CLIENT_ID_CONFIG, "test-client");
    return new KafkaProducer<>(props);
  }

  KafkaConsumer<String,String> newConsumer() {
    final Properties props = new Properties();
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer());
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "test-client");
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test-client");
    props.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
    props.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "10000");
    props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return new KafkaConsumer<>(props);
  }

  void sendAndWait(KafkaProducer<String, String> producer, String topic, String key, String value) {
    try {
      final ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
      producer.send(record).get(lifecycleDuration.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception err) {
      throw new RuntimeException(err);
    }
  }

  protected static List<String> logEntries(MockSpan mockSpan) {
    return mockSpan.logEntries().stream()
        .flatMap(it -> it
            .fields()
            .entrySet().stream()
        )
        .map(it -> it.getKey() + "=" + it.getValue())
        .sorted()
        .collect(Collectors.toList());
  }
}
