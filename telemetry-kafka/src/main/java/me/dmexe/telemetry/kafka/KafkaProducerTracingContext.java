package me.dmexe.telemetry.kafka;

import org.apache.kafka.clients.producer.Callback;

public interface KafkaProducerTracingContext {
  Callback callback();

  Callback callback(Callback callback);
}
