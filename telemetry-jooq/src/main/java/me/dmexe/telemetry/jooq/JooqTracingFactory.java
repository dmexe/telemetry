package me.dmexe.telemetry.jooq;

import io.opentracing.Tracer;
import org.jooq.impl.CallbackExecuteListener;

public interface JooqTracingFactory {
  JooqTracingFactory tracer(Tracer tracer);

  CallbackExecuteListener createListener();

  static JooqTracingFactory newFactory() {
    return new DefaultJooqTracingFactory();
  }
}
