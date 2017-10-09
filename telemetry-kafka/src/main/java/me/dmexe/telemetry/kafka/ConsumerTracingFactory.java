package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ConsumerTracingFactory {
  ConsumerTracingFactory tracer(Tracer tracer);

  ConsumerTracingContext create(ConsumerRecord<?,?> record);

  static ConsumerTracingFactory newFactory() {
    return new DefaultConsumerTracingFactory();
  }
}