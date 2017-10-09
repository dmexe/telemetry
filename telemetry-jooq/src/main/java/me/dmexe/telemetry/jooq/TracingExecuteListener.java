package me.dmexe.telemetry.jooq;

import static me.dmexe.telemetry.jooq.Constants.NULL_NANO;

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

class TracingExecuteListener {
  private final Counter total;
  private final Histogram latency;
  private final Tracer tracer;
  private long startTime = NULL_NANO;

  @Nullable
  private Span span;

  TracingExecuteListener(Tracer tracer, Counter total, Histogram latency) {
    Objects.requireNonNull(total, "total cannot be null");
    Objects.requireNonNull(latency, "latency cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.total = total;
    this.latency = latency;
    this.tracer = tracer;
  }

  void onStart(ExecuteContext ctx) {
    this.span = buildSpan(ctx);
    this.startTime = System.nanoTime();
  }

  void onEnd(ExecuteContext ctx) {
    if (this.span != null) {
      if (ctx.sql() != null) {
        Tags.DB_STATEMENT.set(span, ctx.sql());
      }
      span.finish();
      this.span = null;
    }

    if (startTime != NULL_NANO) {
      long endTime = System.nanoTime();
      String opName = ctx.type().name();
      total.labels(opName).inc();
      latency.labels(opName).observe(SimpleTimer.elapsedSecondsFromNanos(startTime, endTime));
      this.startTime = NULL_NANO;
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
}
