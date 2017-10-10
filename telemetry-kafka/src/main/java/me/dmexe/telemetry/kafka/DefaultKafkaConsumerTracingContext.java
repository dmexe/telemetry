package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.KafkaConstants.COMPONENT_NAME;
import static me.dmexe.telemetry.kafka.KafkaConstants.ERROR_KIND_LOG_NAME;
import static me.dmexe.telemetry.kafka.KafkaConstants.ERROR_MESSAGE_LOG_NAME;
import static me.dmexe.telemetry.kafka.KafkaConstants.MDC_KEY;
import static me.dmexe.telemetry.kafka.KafkaConstants.MDC_OFFSET;
import static me.dmexe.telemetry.kafka.KafkaConstants.MDC_PARTITION;
import static me.dmexe.telemetry.kafka.KafkaConstants.MDC_TOPIC;
import static me.dmexe.telemetry.kafka.KafkaConstants.RECORD_KEY;
import static me.dmexe.telemetry.kafka.KafkaConstants.RECORD_OFFSET;
import static me.dmexe.telemetry.kafka.KafkaConstants.RECORD_PARTITION;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;

class DefaultKafkaConsumerTracingContext<K,V> implements KafkaConsumerTracingContext<K,V> {
  private final ConsumerRecord<K,V> record;
  private final Tracer tracer;
  private final Span span;
  private final Map<String,String> mdc;

  DefaultKafkaConsumerTracingContext(Tracer tracer, ConsumerRecord<K,V> record) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    Objects.requireNonNull(record, "record cannot be null");
    this.tracer = tracer;
    this.record = record;
    this.span = createSpan(tracer, record);
    this.mdc = createMdc(record);
  }

  @Override
  public Span span() {
    return span;
  }

  @Override
  public Map<String, String> mdc() {
    return mdc;
  }

  @Override
  public void handleException(Throwable err) {
    Objects.requireNonNull(err, "err cannot be null");

    final Map<String,String> map = new HashMap<>(2);
    map.put(ERROR_KIND_LOG_NAME, err.getClass().getName());
    map.put(ERROR_MESSAGE_LOG_NAME, err.getMessage());
    Tags.ERROR.set(span, true);
    span.log(map);
  }

  @Override
  public Runnable decorateConsumer(Consumer<ConsumerRecord<K, V>> consumer) {
    return () -> {
      MDC.setContextMap(mdc);
      try (ActiveSpan ignored = tracer.makeActive(span)) {
        consumer.accept(record);
      } catch (Exception err) {
        handleException(err);
        throw err;
      } finally {
        MDC.clear();
      }
    };
  }

  @Override
  public <T> Supplier<T> decorateFunction(Function<ConsumerRecord<K, V>, T> func) {
    return () -> {
      MDC.setContextMap(mdc);
      try (ActiveSpan ignored = tracer.makeActive(span)) {
        return func.apply(record);
      } catch (Exception err) {
        handleException(err);
        throw err;
      } finally {
        MDC.clear();
      }
    };
  }

  private static Map<String,String> createMdc(ConsumerRecord<?,?> record) {
    final Map<String,String> mdc = new HashMap<>();

    mdc.put(MDC_TOPIC, record.topic());
    mdc.put(MDC_PARTITION, Integer.toString(record.partition()));
    mdc.put(MDC_OFFSET, Long.toString(record.offset()));

    if (record.key() != null) {
      mdc.put(MDC_KEY, record.key().toString());
    }

    return Collections.unmodifiableMap(mdc);
  }

  private static Span createSpan(Tracer tracer, ConsumerRecord<?,?> record) {
    final Span span = tracer
        .buildSpan("kafka.consume " + record.topic())
        .startManual();

    Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CONSUMER);
    Tags.COMPONENT.set(span, COMPONENT_NAME);
    Tags.MESSAGE_BUS_DESTINATION.set(span, record.topic());
    RECORD_PARTITION.set(span, record.partition());
    RECORD_OFFSET.set(span, Long.toString(record.offset()));

    if (record.key() != null) {
      RECORD_KEY.set(span, record.key().toString());
    }

    return span;
  }
}
