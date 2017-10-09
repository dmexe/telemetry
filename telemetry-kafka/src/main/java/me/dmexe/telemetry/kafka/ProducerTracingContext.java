package me.dmexe.telemetry.kafka;

import org.apache.kafka.clients.producer.Callback;

public interface ProducerTracingContext {
  Callback callback();

  Callback callback(Callback callback);
}
