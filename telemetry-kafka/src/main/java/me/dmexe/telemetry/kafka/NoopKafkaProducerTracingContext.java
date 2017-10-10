package me.dmexe.telemetry.kafka;

import org.apache.kafka.clients.producer.Callback;

class NoopKafkaProducerTracingContext implements KafkaProducerTracingContext {

  @Override
  public Callback callback() {
    return (metadata, exception) -> {
    };
  }

  @Override
  public Callback callback(Callback callback) {
    return callback;
  }
}
