package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.Constants.ERROR_KIND_LOG_NAME;
import static me.dmexe.telemetry.kafka.Constants.ERROR_MESSAGE_LOG_NAME;
import static me.dmexe.telemetry.kafka.Constants.RECORD_KEY_SIZE;
import static me.dmexe.telemetry.kafka.Constants.RECORD_OFFSET;
import static me.dmexe.telemetry.kafka.Constants.RECORD_PARTITION;
import static me.dmexe.telemetry.kafka.Constants.RECORD_VALUE_SIZE;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

class DefaultProducerTracingContext implements ProducerTracingContext {
  private final Span span;

  DefaultProducerTracingContext(Span span) {
    Objects.requireNonNull(span, "span cannot be null");
    this.span = span;
  }

  @Override
  public Callback callback() {
    return (metadata, exception) -> {
      if (metadata != null) {
        handleMetadata(metadata);
      }
      if (exception != null) {
        handleException(exception);
      }
      span.finish();
    };
  }

  @Override
  public Callback callback(Callback callback) {
    return (metadata, exception) -> {
      try {
        if (metadata != null) {
          handleMetadata(metadata);
        }
        if (exception != null) {
          handleException(exception);
        }
        callback.onCompletion(metadata, exception);
      } finally {
        span.finish();
      }
    };
  }

  private void handleException(Exception exception) {
    final Map<String,String> map = new HashMap<>(2);
    map.put(ERROR_KIND_LOG_NAME, exception.getClass().getName());
    map.put(ERROR_MESSAGE_LOG_NAME, exception.getMessage());
    Tags.ERROR.set(span, true);
    span.log(map);
  }

  private void handleMetadata(RecordMetadata metadata) {
    RECORD_PARTITION.set(span, metadata.partition());
    RECORD_OFFSET.set(span, Long.toString(metadata.offset()));

    if (metadata.serializedKeySize() >= 0) {
      RECORD_KEY_SIZE.set(span, metadata.serializedKeySize());
    }

    if (metadata.serializedValueSize() >= 0) {
      RECORD_VALUE_SIZE.set(span, metadata.serializedValueSize());
    }
  }
}
