package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.Constants.MDC_KEY;
import static me.dmexe.telemetry.kafka.Constants.MDC_OFFSET;
import static me.dmexe.telemetry.kafka.Constants.MDC_PARTITION;
import static me.dmexe.telemetry.kafka.Constants.MDC_TOPIC;
import static me.dmexe.telemetry.kafka.Constants.RECORD_KEY;
import static me.dmexe.telemetry.kafka.Constants.RECORD_OFFSET;
import static me.dmexe.telemetry.kafka.Constants.RECORD_PARTITION;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.Nullable;

class DefaultConsumerTracingFactory implements ConsumerTracingFactory {

  @Nullable
  private Tracer tracer;

  DefaultConsumerTracingFactory() {
    this.tracer = null;
  }

  @Override
  public ConsumerTracingFactory tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public ConsumerTracingContext create(ConsumerRecord<?, ?> record) {
    Objects.requireNonNull(record, "record cannot be null");

    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    final Map<String,String> mdc = new HashMap<>();

    final Span span = tracer
        .buildSpan("kafka.consume " + record.topic())
        .startManual();

    Tags.MESSAGE_BUS_DESTINATION.set(span, record.topic());
    RECORD_PARTITION.set(span, record.partition());
    RECORD_OFFSET.set(span, Long.toString(record.offset()));

    mdc.put(MDC_TOPIC, record.topic());
    mdc.put(MDC_PARTITION, Integer.toString(record.partition()));
    mdc.put(MDC_OFFSET, Long.toString(record.offset()));

    if (record.key() != null) {
      RECORD_KEY.set(span, record.key().toString());
      mdc.put(MDC_KEY, record.key().toString());
    }

    return new DefaultConsumerTracingContext(span, mdc);
  }
}
