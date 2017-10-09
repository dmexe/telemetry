package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ConsumerTracingFactory<K,V> {
  ConsumerTracingFactory<K,V> tracer(Tracer tracer);

  ConsumerTracingContext<K,V> create(ConsumerRecord<K,V> record);

  static <K,V> ConsumerTracingFactory<K,V> newFactory(Class<K> keyClass, Class<V> valueClass) {
    return new DefaultConsumerTracingFactory<>();
  }
}