package me.dmexe.telemetry.jooq;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.jooq.impl.CallbackExecuteListener;

class DefaultJooqTracingFactory implements JooqTracingFactory {

  private static final Counter.Builder totalBuilder = Counter.build()
      .namespace("jooq")
      .name("started_total")
      .labelNames("sql_op").help("A total number of database queries were been started.");

  private static final Histogram.Builder latencyBuilder = Histogram.build()
      .namespace("jooq")
      .name("latency_seconds")
      .labelNames("sql_op")
      .help("A seconds which spend to execute database queries.");

  @Nullable
  private Tracer tracer;

  @Nullable
  private CollectorRegistry collectorRegistry;

  private static class Lazy {
    private static final Counter total = totalBuilder.register();

    private static final Histogram latency = latencyBuilder.register();
  }

  DefaultJooqTracingFactory() {
  }

  JooqTracingFactory collectorRegistry(CollectorRegistry collectorRegistry) {
    Objects.requireNonNull(collectorRegistry, "collectorRegistry cannot be null");
    this.collectorRegistry = collectorRegistry;
    return this;
  }

  @Override
  public JooqTracingFactory tracer(Tracer tracer) {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    this.tracer = tracer;
    return this;
  }

  @Override
  public CallbackExecuteListener createListener() {
    Tracer tracer;
    if (this.tracer == null) {
      tracer = GlobalTracer.get();
    } else {
      tracer = this.tracer;
    }

    JooqTracingExecuteListener listener;
    if (this.collectorRegistry == null) {
      listener = new JooqTracingExecuteListener(
          tracer,
          Lazy.total,
          Lazy.latency);
    } else {
      listener = new JooqTracingExecuteListener(
          tracer,
          totalBuilder.register(collectorRegistry),
          latencyBuilder.register(collectorRegistry));
    }

    return new CallbackExecuteListener()
        .onStart(listener::onStart)
        .onEnd(listener::onEnd);
  }
}
