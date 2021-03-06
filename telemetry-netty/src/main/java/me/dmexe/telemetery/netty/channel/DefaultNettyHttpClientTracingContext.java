package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.NettyConstants.ERROR_KIND_LOG_NAME;
import static me.dmexe.telemetery.netty.channel.NettyConstants.ERROR_MESSAGE_LOG_NAME;
import static me.dmexe.telemetery.netty.channel.NettyConstants.HTTP_COMPONENT_NAME;
import static me.dmexe.telemetery.netty.channel.NettyConstants.HTTP_CONTENT_LENGTH;
import static me.dmexe.telemetery.netty.channel.NettyConstants.HTTP_CONTENT_TYPE;
import static me.dmexe.telemetery.netty.channel.NettyConstants.WIRE_RECV;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleTimer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

class DefaultNettyHttpClientTracingContext implements NettyHttpTracingContext {
  private static final long NULL_NANO = -1L;

  private final String address;
  private final Tracer tracer;
  private final Ticker ticker;
  private final Counter handledCounter;
  private final Histogram latencyHistogram;
  private long requestStartTimeNanos;

  @Nullable
  private Span span;

  @Nullable
  private HttpMethod method;

  @Nullable
  private String code;

  DefaultNettyHttpClientTracingContext(
      String address,
      Tracer tracer,
      Ticker ticker,
      Counter handledCounter,
      Histogram latencyHistogram) {
    Objects.requireNonNull(address, "address cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");
    Objects.requireNonNull(ticker, "ticker cannot be null");
    Objects.requireNonNull(handledCounter, "handledCounter cannot be null");
    Objects.requireNonNull(latencyHistogram, "latencyHistogram cannot be null");

    this.address = address;
    this.ticker = ticker;
    this.tracer = tracer;
    this.handledCounter = handledCounter;
    this.latencyHistogram = latencyHistogram;
    this.span = null;
    this.method = null;
    this.requestStartTimeNanos = NULL_NANO;
  }

  @Override
  public void handleRequest(HttpRequest request, Channel channel) {
    requestStartTimeNanos = ticker.nanoTime();
    method = request.method();

    final SpanContext spanContext = NettyHttpTracingContext.getClientParentContext(channel);
    if (spanContext != null) {
      span = tracer
          .buildSpan("http." + request.method().name())
          .asChildOf(spanContext)
          .startManual();
    }

    if (span != null) {
      tracer.inject(span.context(), Builtin.HTTP_HEADERS, new NettyHttpRequestCarrier(request));

      Tags.COMPONENT.set(span, HTTP_COMPONENT_NAME);
      Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
      Tags.HTTP_URL.set(span, request.uri());
      Tags.HTTP_METHOD.set(span, request.method().name());

      new InetAddressResolver(channel.remoteAddress()).setPeerAddress(span);
    }
  }

  @Override
  public void handleResponse(HttpResponse response) {
    code = Integer.toString(response.status().code());

    if (span != null) {
      span.log(WIRE_RECV);
      Tags.HTTP_STATUS.set(span, response.status().code());

      final String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
      if (contentType != null) {
        HTTP_CONTENT_TYPE.set(span, contentType);
      }

      final Integer contentLength = response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH);
      if (contentLength != null) {
        HTTP_CONTENT_LENGTH.set(span, contentLength);
      }
    }
  }

  @Override
  public void completed() {
    if (requestStartTimeNanos != NULL_NANO && method != null && code != null) {
      handledCounter
          .labels(address, code, method.name())
          .inc();
      latencyHistogram
          .labels(address, code, method.name())
          .observe(SimpleTimer.elapsedSecondsFromNanos(requestStartTimeNanos, ticker.nanoTime()));

      requestStartTimeNanos = NULL_NANO;
      method = null;
      code = null;
    }

    if (span != null) {
      span.finish();
      span = null;
    }
  }

  @Override
  public void exceptionCaught(Throwable err) {
    if (span != null) {
      final Map<String,String> log = new HashMap<>();
      log.put(ERROR_KIND_LOG_NAME, err.getClass().getName());
      log.put(ERROR_MESSAGE_LOG_NAME, err.getMessage());
      Tags.ERROR.set(span, true);
      span.log(log);
    }
  }

  @Override
  public void exceptionCaught(String err) {
    if (span != null) {
      final Map<String,String> log = new HashMap<>();
      log.put(ERROR_KIND_LOG_NAME, err.getClass().getName());
      log.put(ERROR_MESSAGE_LOG_NAME, err);
      Tags.ERROR.set(span, true);
      span.log(log);
    }
  }
}
