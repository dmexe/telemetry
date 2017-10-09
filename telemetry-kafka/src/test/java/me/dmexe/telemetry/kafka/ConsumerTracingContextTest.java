package me.dmexe.telemetry.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import java.time.Duration;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumerTracingContextTest extends TestEnv {
  private MockTracer tracer;
  private ConsumerTracingFactory tracingFactory;
  private final String topic = ConsumerMetricsCollectorTest.class.getSimpleName();
  private final Duration subscribeTimeout = Duration.ofSeconds(30);

  @BeforeEach
  void before() {
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());
    tracingFactory = ConsumerTracingFactory.newFactory().tracer(tracer);
  }

  @Test
  void should_handle_context() {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> {
        sendAndWait(producer, topic, "key", "value");
      });

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext ctx = tracingFactory.create(record);
          assertThat(ctx.mdc())
              .isNotEmpty()
              .containsEntry("kafka:topic", record.topic())
              .containsEntry("kafka:partition", Integer.toString(record.partition()))
              .containsEntry("kafka:offset", Long.toString(record.offset()))
              .containsEntry("kafka:key", record.key());
          ctx.span().finish();
        }
      });

      consumer.commitSync();
    }

    assertThat(tracer.finishedSpans()).isNotEmpty();
    final MockSpan span = tracer.finishedSpans().get(tracer.finishedSpans().size() - 1);
    assertThat(span.tags())
        .containsOnlyKeys(
            "kafka.key",
            "kafka.offset",
            "kafka.partition",
            "message_bus.destination");
  }

  @Test
  void should_handle_context_with_error() {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> {
        sendAndWait(producer, topic, "key", "value");
      });

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext ctx = tracingFactory.create(record);
          ctx.handleException(new RuntimeException("boom"));
          ctx.span().finish();
        }
      });

      consumer.commitSync();
    }

    assertThat(tracer.finishedSpans()).isNotEmpty();
    final MockSpan span = tracer.finishedSpans().get(tracer.finishedSpans().size() - 1);
    assertThat(logEntries(span))
        .containsExactly(
            "error.kind=java.lang.RuntimeException",
            "error.message=boom");
  }
}
