package me.dmexe.telemetry.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaProducerTracingContextTest extends TestEnv {
  private MockTracer tracer;
  private KafkaProducerTracingFactory tracingFactory;
  private final String topic = KafkaConsumerMetricsCollectorTest.class.getSimpleName();

  @BeforeEach
  void before() {
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());
    tracingFactory = KafkaProducerTracingFactory.newFactory().tracer(tracer);
  }

  @Test
  void should_handle_record() throws Exception {
    final MockSpan root = tracer.buildSpan("root").startManual();

    try(ActiveSpan ignored = tracer.makeActive(root);
        KafkaProducer<String,String> producer = newProducer()) {
      final ProducerRecord<String,String> record =
          new ProducerRecord<>(topic, "key", "value");
      final KafkaProducerTracingContext ctx = tracingFactory.create(record);
      producer.send(record, ctx.callback()).get(3, TimeUnit.SECONDS);
    }

    assertThat(tracer.finishedSpans()).isNotEmpty();

    final MockSpan span = tracer.finishedSpans().get(0);
    assertThat(span.tags())
        .containsEntry("component", "kafka")
        .containsEntry("span.kind", "producer")
        .containsKeys(
            "kafka.key",
            "kafka.key_size",
            "kafka.offset",
            "kafka.partition",
            "kafka.value_size",
            "message_bus.destination");

    assertThat(root.context().traceId())
        .isEqualTo(span.context().traceId());
  }

  @Test
  void should_handle_record_with_callback() throws Exception {
    final AtomicBoolean callback = new AtomicBoolean(false);
    final MockSpan root = tracer.buildSpan("root").startManual();

    try(ActiveSpan ignored = tracer.makeActive(root);
        KafkaProducer<String,String> producer = newProducer()) {
      final ProducerRecord<String,String> record =
          new ProducerRecord<>(topic, "key", "value");
      final KafkaProducerTracingContext ctx = tracingFactory.create(record);
      producer
          .send(record, ctx.callback((meta, err) -> callback.set(true)))
          .get(3, TimeUnit.SECONDS);
    }

    assertThat(callback.get()).isTrue();
    assertThat(tracer.finishedSpans()).isNotEmpty();

    final MockSpan span = tracer.finishedSpans().get(0);
    assertThat(span.tags()).containsKeys(
        "kafka.key",
        "kafka.key_size",
        "kafka.offset",
        "kafka.partition",
        "kafka.value_size",
        "message_bus.destination");
    assertThat(root.context().traceId())
        .isEqualTo(span.context().traceId());
  }

  @Test
  void should_handle_with_no_op_context() throws Exception {
    try(KafkaProducer<String,String> producer = newProducer()) {
      final ProducerRecord<String,String> record =
          new ProducerRecord<>(topic, "key", "value");
      final KafkaProducerTracingContext ctx = tracingFactory.create(record);
      producer.send(record, ctx.callback()).get(3, TimeUnit.SECONDS);
    }

    assertThat(tracer.finishedSpans()).isEmpty();
  }
}
