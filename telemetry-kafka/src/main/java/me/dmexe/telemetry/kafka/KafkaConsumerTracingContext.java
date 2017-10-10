package me.dmexe.telemetry.kafka;

import io.opentracing.Span;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface KafkaConsumerTracingContext<K,V> {
  Span span();

  Map<String,String> mdc();

  void handleException(Throwable err);

  void finish();

  Runnable decorateConsumer(Consumer<ConsumerRecord<K,V>> consumer);

  <T> Supplier<T> decorateFunction(Function<ConsumerRecord<K,V>, T> func);
}
