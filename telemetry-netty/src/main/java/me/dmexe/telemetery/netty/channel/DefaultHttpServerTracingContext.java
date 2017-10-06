package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.Constants.ERROR_KIND_LOG_NAME;
import static me.dmexe.telemetery.netty.channel.Constants.ERROR_MESSAGE_LOG_NAME;
import static me.dmexe.telemetery.netty.channel.Constants.HTTP_COMPONENT_NAME;
import static me.dmexe.telemetery.netty.channel.Constants.HTTP_CONTENT_LENGTH;
import static me.dmexe.telemetery.netty.channel.Constants.HTTP_CONTENT_TYPE;
import static me.dmexe.telemetery.netty.channel.Constants.PEER_ADDRESS;
import static me.dmexe.telemetery.netty.channel.Constants.SERVER_SEND_LOG_NAME;

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

class DefaultHttpServerTracingContext implements HttpTracingContext {
  private static long NULL_NANO = -1L;

  private final String address;
  private final Tracer tracer;
  private final Ticker ticker;
  private final Counter handledCounter;
  private final Histogram latencyHistogram;
  private long requestStartTimeNanos;

  @Nullable
  private Span span;

  @Nullable
  private String code;

  @Nullable
  private HttpMethod method;

  DefaultHttpServerTracingContext(
      String address,
      Tracer tracer,
      Ticker ticker,
      Counter handledCounter,
      Histogram latencyHistogram) {
    Objects.requireNonNull(address, "address cannot be null");
    Objects.requireNonNull(tracer, "tracer cannot be null");
    Objects.requireNonNull(ticker, "ticker cannot be null");
    Objects.requireNonNull(latencyHistogram, "latencyHistogram cannot be null");
    Objects.requireNonNull(handledCounter, "handledCounter cannot be null");
    this.address = address;
    this.tracer = tracer;
    this.ticker = ticker;
    this.latencyHistogram = latencyHistogram;
    this.handledCounter = handledCounter;
    this.requestStartTimeNanos = NULL_NANO;
    this.span = null;
  }

  @Override
  public void handleRequest(HttpRequest request, Channel channel) {
    requestStartTimeNanos = ticker.nanoTime();
    method = request.method();
    span = createSpan(request, channel.remoteAddress());
    code = null;

    if (span != null) {
      HttpTracingContext.addServerCurrentSpan(channel, span);
    }
  }

  @Override
  public void handleResponse(HttpResponse response, Channel channel) {
    code = Integer.toString(response.status().code());
    if (span != null) {
      span.log(SERVER_SEND_LOG_NAME);

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
    if (requestStartTimeNanos != NULL_NANO && code != null && method != null) {
      handledCounter
          .labels(address, code, method.name())
          .inc();
      latencyHistogram
          .labels(address, code, method.name())
          .observe(SimpleTimer.elapsedSecondsFromNanos(requestStartTimeNanos, ticker.nanoTime()));

      this.requestStartTimeNanos = NULL_NANO;
      this.code = null;
      this.method = null;
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

  private Span createSpan(HttpRequest request, SocketAddress remoteAddress) {
    final SpanContext parentSpanCtx = tracer.extract(
        Builtin.HTTP_HEADERS,
        new HttpRequestCarrier(request));

    final String operationName = "http." + request.method().name();

    Span span;

    if (parentSpanCtx == null) {
      span = tracer.buildSpan(operationName).startManual();
    } else {
      span = tracer.buildSpan(operationName).asChildOf(parentSpanCtx).startManual();
    }

    if (address != null && remoteAddress instanceof InetSocketAddress) {
      final InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
      final InetAddress inetAddress = inetSocketAddress.getAddress();

      if (inetAddress != null) {
        PEER_ADDRESS.set(span, inetAddress.getHostAddress());
        Tags.PEER_PORT.set(span, inetSocketAddress.getPort());
      }
    }

    Tags.HTTP_METHOD.set(span, request.method().name());
    Tags.HTTP_URL.set(span, request.uri());
    Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
    Tags.COMPONENT.set(span, HTTP_COMPONENT_NAME);

    return span;
  }
}
