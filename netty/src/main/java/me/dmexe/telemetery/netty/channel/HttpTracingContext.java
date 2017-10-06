package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.Constants.CLIENT_PARENT_SPAN_CONTEXT;
import static me.dmexe.telemetery.netty.channel.Constants.SERVER_CURRENT_SPAN;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import javax.annotation.Nullable;

public interface HttpTracingContext {

  void handleRequest(HttpRequest request, Channel channel);

  void handleResponse(HttpResponse response, Channel channel);

  void completed();

  void exceptionCaught(Throwable err);

  void exceptionCaught(String err);

  static void addSpan(Channel channel, Span span) {
    channel.attr(SERVER_CURRENT_SPAN).set(span);
  }

  static void addContext(Channel channel, SpanContext spanContext) {
    channel.attr(CLIENT_PARENT_SPAN_CONTEXT).set(spanContext);
  }

  static @Nullable SpanContext getContext(Channel channel) {
    return channel.attr(CLIENT_PARENT_SPAN_CONTEXT).get();
  }
}