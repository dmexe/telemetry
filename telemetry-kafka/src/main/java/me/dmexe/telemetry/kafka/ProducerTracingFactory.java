package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface ProducerTracingFactory {
  ProducerTracingFactory tracer(Tracer tracer);

  ProducerTracingContext create(ProducerRecord<?,?> record);

  static ProducerTracingFactory newFactory() {
    return new DefaultProducerTracingFactory();
  }
}
