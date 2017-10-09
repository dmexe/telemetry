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
import org.slf4j.MDC;

class ConsumerTracingContextTest extends TestEnv {
  private MockTracer tracer;
  private ConsumerTracingFactory<String,String> tracingFactory;
  private final String topic = ConsumerMetricsCollectorTest.class.getSimpleName();
  private final Duration subscribeTimeout = Duration.ofSeconds(30);

  @BeforeEach
  void before() {
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());
    tracingFactory = ConsumerTracingFactory.newFactory(String.class, String.class).tracer(tracer);
  }

  @Test
  void should_handle_context() {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> sendAndWait(producer, topic, "key", "value"));

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext<String,String> ctx = tracingFactory.create(record);
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
        .containsEntry("component", "kafka")
        .containsEntry("span.kind", "consumer")
        .containsKeys(
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

      IntStream.range(0, 10).forEach(n -> sendAndWait(producer, topic, "key", "value"));

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext<String,String> ctx = tracingFactory.create(record);
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

  @Test
  void should_decorate_consumer() {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> sendAndWait(producer, topic, "key", "value"));

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext<String,String> ctx = tracingFactory.create(record);
          ctx
              .decorateConsumer(rec -> {
                assertThat(MDC.getCopyOfContextMap())
                    .isNotEmpty()
                    .containsOnlyKeys(
                        "kafka:key",
                        "kafka:offset",
                        "kafka:partition",
                        "kafka:topic");
                assertThat(tracer.activeSpan())
                    .isNotNull();
              })
              .run();
        }
      });

      consumer.commitSync();
    }

    assertThat(tracer.finishedSpans()).isNotEmpty();
  }

  @Test
  void should_decorate_function() {
    try (
        KafkaProducer<String, String> producer = newProducer();
        KafkaConsumer<String, String> consumer = newConsumer()) {
      consumer.subscribe(newArrayList(topic));

      IntStream.range(0, 10).forEach(n -> sendAndWait(producer, topic, "key", "value"));

      IntStream.range(0, 10).forEach(n -> {
        final ConsumerRecords<String, String> records = consumer.poll(subscribeTimeout.toMillis());
        assertThat(records).isNotEmpty();

        for (ConsumerRecord<String,String> record : records) {
          final ConsumerTracingContext<String,String> ctx = tracingFactory.create(record);
          final boolean res = ctx
              .decorateFunction(rec -> {
                assertThat(MDC.getCopyOfContextMap())
                    .isNotEmpty()
                    .containsOnlyKeys(
                        "kafka:key",
                        "kafka:offset",
                        "kafka:partition",
                        "kafka:topic");
                assertThat(tracer.activeSpan())
                    .isNotNull();

                return true;
              })
              .get();
          assertThat(res).isTrue();
        }
      });

      consumer.commitSync();
    }

    assertThat(tracer.finishedSpans()).isNotEmpty();
  }
}
