package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;

class DefaultKafkaConsumerTracingFactory<K,V> implements KafkaConsumerTracingFactory<K,V> {

  @Nullable
  private Tracer tracer;

  DefaultKafkaConsumerTracingFactory() {
    this.tracer = null;
  }

  @Override
  public KafkaConsumerTracingFactory<K,V> tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public KafkaConsumerTracingContext<K,V> create(ConsumerRecord<K, V> record) {
    Objects.requireNonNull(record, "record cannot be null");

    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    return new DefaultKafkaConsumerTracingContext<>(tracer, record);
  }
}
