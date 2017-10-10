package me.dmexe.telemetry.jooq;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleTimer;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.jooq.ExecuteContext;

class JooqTracingExecuteListener {
  private static final String DATA_KEY = JooqTracingExecuteListener.class.getName();
  private final Counter total;
  private final Histogram latency;
  private final Tracer tracer;

  JooqTracingExecuteListener(Tracer tracer, Counter total, Histogram latency) {
    Objects.requireNonNull(total, "total cannot be null");
    Objects.requireNonNull(latency, "latency cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.total = total;
    this.latency = latency;
    this.tracer = tracer;
  }

  void onStart(ExecuteContext ctx) {
    final Span span = buildSpan(ctx);
    final long startTime = System.nanoTime();
    final ContextData contextData = new ContextData(span, startTime);
    ctx.data(DATA_KEY, contextData);
  }

  void onEnd(ExecuteContext ctx) {
    final Object data = ctx.data(DATA_KEY);
    if (data == null || !(data instanceof ContextData)) {
      return;
    }

    final ContextData contextData = (ContextData) data;

    long endTime = System.nanoTime();
    String opName = ctx.type().name();
    total.labels(opName).inc();
    final double elapsed = SimpleTimer.elapsedSecondsFromNanos(contextData.startTime, endTime);
    latency.labels(opName).observe(elapsed);

    if (contextData.span != null) {
      if (ctx.sql() != null) {
        Tags.DB_STATEMENT.set(contextData.span, ctx.sql());
      }
      contextData.span.finish();
    }
  }

  @Nullable
  private Span buildSpan(ExecuteContext ctx) {
    final ActiveSpan parent = tracer.activeSpan();
    if (parent == null) {
      return null;
    }

    return tracer
        .buildSpan("sql." + ctx.type().name())
        .withTag(Tags.DB_TYPE.getKey(), ctx.dialect().getName())
        .withTag(Tags.COMPONENT.getKey(), "jooq")
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
        .asChildOf(parent)
        .startManual();
  }

  private static class ContextData {
    private final Span span;
    private final long startTime;

    ContextData(@Nullable Span span, long startTime) {
      this.span = span;
      this.startTime = startTime;
    }
  }
}
