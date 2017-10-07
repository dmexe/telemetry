package me.dmexe.telemetery.netty.channel;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.IntTag;
import io.opentracing.tag.StringTag;

class Constants {
  static final String CLIENT_CONNECTION_CLOSED =
      "the client closed the connection before the server answered the request";
  static final String SERVER_CONNECTION_CLOSED =
      "the server closed the connection before the client received the response";

  static final HttpResponse CLIENT_CONNECTION_CLOSED_RESPONSE =
      new DefaultHttpResponse(HTTP_1_1,
          new HttpResponseStatus(499, "Client connection closed"));

  static final HttpResponse SERVER_CONNECTION_CLOSED_RESPONSE =
      new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);

  static final String CLIENT_SUBSYSTEM = "client";
  static final String SERVER_SUBSYSTEM = "server";

  static final AttributeKey<Span> SERVER_CURRENT_SPAN =
      AttributeKey.newInstance("SERVER_CURRENT_SPAN");
  static final AttributeKey<SpanContext> CLIENT_PARENT_SPAN_CONTEXT =
      AttributeKey.newInstance("CLIENT_PARENT_SPAN_CONTEXT");

  static final String HTTP_COMPONENT_NAME = "netty";

  static final StringTag HTTP_CONTENT_TYPE = new StringTag("http.content_type");
  static final IntTag HTTP_CONTENT_LENGTH = new IntTag("http.content_length");
  static final StringTag PEER_ADDRESS = new StringTag("peer.address");

  static final String SERVER_SEND_LOG_NAME = "ss";
  static final String CLIENT_RECEIVE_LOG_NAME = "cr";

  static final String ERROR_KIND_LOG_NAME = "error.kind";
  static final String ERROR_MESSAGE_LOG_NAME = "error.message";
}
