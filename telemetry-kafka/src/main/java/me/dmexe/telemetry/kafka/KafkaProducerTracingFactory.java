package me.dmexe.telemetry.kafka;

import io.opentracing.Tracer;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface KafkaProducerTracingFactory {
  KafkaProducerTracingFactory tracer(Tracer tracer);

  KafkaProducerTracingContext create(ProducerRecord<?,?> record);

  static KafkaProducerTracingFactory newFactory() {
    return new DefaultKafkaProducerTracingFactory();
  }
}
