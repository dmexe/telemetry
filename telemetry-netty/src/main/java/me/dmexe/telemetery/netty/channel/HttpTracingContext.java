package me.dmexe.telemetery.netty.channel;

import static me.dmexe.telemetery.netty.channel.Constants.CLIENT_PARENT_SPAN_CONTEXT;
import static me.dmexe.telemetery.netty.channel.Constants.SERVER_CURRENT_SPAN;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import org.jetbrains.annotations.Nullable;

public interface HttpTracingContext {

  void handleRequest(HttpRequest request, Channel channel);

  void handleResponse(HttpResponse response);

  void completed();

  void exceptionCaught(Throwable err);

  void exceptionCaught(String err);

  static void addServerCurrentSpan(Channel channel, Span span) {
    channel.attr(SERVER_CURRENT_SPAN).set(span);
  }

  static void addClientParentContext(Channel channel, SpanContext spanParentContext) {
    channel.attr(CLIENT_PARENT_SPAN_CONTEXT).set(spanParentContext);
  }

  static @Nullable SpanContext getClientParentContext(Channel channel) {
    return channel.attr(CLIENT_PARENT_SPAN_CONTEXT).get();
  }
}