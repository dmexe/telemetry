package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.KafkaConstants.COMPONENT_NAME;
import static me.dmexe.telemetry.kafka.KafkaConstants.RECORD_KEY;
import static me.dmexe.telemetry.kafka.KafkaConstants.RECORD_PARTITION;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jetbrains.annotations.Nullable;

class DefaultKafkaProducerTracingFactory implements KafkaProducerTracingFactory {

  @Nullable
  private Tracer tracer;

  DefaultKafkaProducerTracingFactory() {
  }

  @Override
  public KafkaProducerTracingFactory tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public KafkaProducerTracingContext create(ProducerRecord<?, ?> record) {
    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    final ActiveSpan parent = tracer.activeSpan();
    if (parent == null) {
      return new NoopKafkaProducerTracingContext();
    }

    final Span span = tracer
        .buildSpan("kafka.send")
        .asChildOf(parent)
        .startManual();

    final String topic = record.topic();
    if (topic != null) {
      Tags.MESSAGE_BUS_DESTINATION.set(span, topic);
    }

    final Integer partition = record.partition();
    if (partition != null) {
      RECORD_PARTITION.set(span, partition);
    }

    final Object key = record.key();
    if (key != null) {
      RECORD_KEY.set(span, key.toString());
    }

    Tags.COMPONENT.set(span, COMPONENT_NAME);
    Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_PRODUCER);

    return new DefaultKafkaProducerTracingContext(span);
  }
}
