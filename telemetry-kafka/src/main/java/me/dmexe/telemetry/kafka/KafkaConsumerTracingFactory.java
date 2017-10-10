package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface KafkaConsumerTracingFactory<K,V> {
  KafkaConsumerTracingFactory<K,V> tracer(Tracer tracer);

  KafkaConsumerTracingContext<K,V> create(ConsumerRecord<K,V> record);

  static <K,V> KafkaConsumerTracingFactory<K,V> newFactory(Class<K> keyClass, Class<V> valueClass) {
    return new DefaultKafkaConsumerTracingFactory<>();
  }
}