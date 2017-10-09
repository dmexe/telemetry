package me.dmexe.telemetry.kafka;

import io.opentracing.Span;
import java.util.Map;

public interface ConsumerTracingContext {
  Span span();

  Map<String,String> mdc();

  void handleException(Throwable err);
}
