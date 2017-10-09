package me.dmexe.telemetry.kafka;

import static me.dmexe.telemetry.kafka.Constants.ERROR_KIND_LOG_NAME;
import static me.dmexe.telemetry.kafka.Constants.ERROR_MESSAGE_LOG_NAME;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class DefaultConsumerTracingContext implements ConsumerTracingContext {
  private final Span span;
  private final Map<String,String> mdc;

  DefaultConsumerTracingContext(Span span, Map<String,String> mdc) {
    Objects.requireNonNull(span, "span cannot be null");
    this.span = span;
    this.mdc = Collections.unmodifiableMap(mdc);
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
}
