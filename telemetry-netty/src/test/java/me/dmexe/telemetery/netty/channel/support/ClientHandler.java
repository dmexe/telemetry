package me.dmexe.telemetery.netty.channel.support;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.Objects;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClientHandler extends SimpleChannelInboundHandler<HttpObject> {
  private final Logger log = LoggerFactory.getLogger(ClientHandler.class);
  private final Queue<HttpResponse> queue;
  private HttpResponse response = null;

  ClientHandler(Queue<HttpResponse> queue) {
    Objects.requireNonNull(queue, "queue cannot be null");
    this.queue = queue;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    log.info("handle client response {}", msg);

    if (msg instanceof HttpResponse) {
      this.response = (HttpResponse) msg;
    }

    if (msg instanceof HttpContent) {
      HttpContent content = (HttpContent) msg;
      log.info("client content received: {}", content.content().readableBytes());
    }

    if (msg instanceof LastHttpContent) {
      log.info("client received last line");
      if (response != null) {
        queue.add(response);
        this.response = null;
      }
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    log.info("client read complete");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("error at client channel: {}", cause.getMessage(), cause);
    super.exceptionCaught(ctx, cause);
  }
}
