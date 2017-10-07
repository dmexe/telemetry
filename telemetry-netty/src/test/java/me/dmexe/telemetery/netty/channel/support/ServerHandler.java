package me.dmexe.telemetery.netty.channel.support;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerHandler extends ChannelInboundHandlerAdapter {
  private static Logger log = LoggerFactory.getLogger(ServerHandler.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof FullHttpRequest) {
      final FullHttpRequest request = (FullHttpRequest) msg;
      log.info("handle server request {} {}", request.method(), request.uri());

      if (is100ContinueExpected(request)) {
        ctx.writeAndFlush(
            new DefaultFullHttpResponse(
                HTTP_1_1,
                HttpResponseStatus.CONTINUE));
      }

      if (request.uri().startsWith("/ping")) {
        handlePing(ctx);
      } else if (request.uri().startsWith("/file")) {
        handleFile(ctx);
      } else if (request.uri().startsWith("/server/error")) {
        throw new RuntimeException("boom");
      } else if (request.uri().startsWith("/sleep/second")) {
        handleSleepSeconds(ctx);
      } else {
        handleNotFound(ctx);
      }
    } else {
      super.channelRead(ctx, msg);
    }
  }

  private void handleSleepSeconds(ChannelHandlerContext ctx) throws Exception {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException err) {
        throw new RuntimeException(err);
      }
      handlePing(ctx);
    });
    thread.setDaemon(true);
    thread.start();
  }

  private void handleNotFound(ChannelHandlerContext ctx) {
    final HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
    setContentLength(response, 0);
    ctx.writeAndFlush(response);
  }

  private void handleFile(ChannelHandlerContext ctx) throws IOException {
    final HttpResponse response = new DefaultHttpResponse( HTTP_1_1, OK);
    final URL fileName = getClass().getClassLoader().getResource("8k.file");
    if (fileName == null) {
      handleNotFound(ctx);
      return;
    }

    final Path path = Paths.get(fileName.getPath());
    final FileChannel fc = FileChannel.open(path);
    final long bytes = Files.size(path);

    setContentLength(response, bytes);
    ctx.write(response);
    final FileRegion region = new DefaultFileRegion(fc, 0, bytes);
    final ChannelFuture future = ctx.write(region, ctx.newPromise());
    future.addListener(sendFileFuture -> {
      if (!sendFileFuture.isSuccess()) {
        log.info("sendFile error", sendFileFuture.cause());
      }

      if (sendFileFuture.isSuccess()) {
        ctx.writeAndFlush(EMPTY_LAST_CONTENT);
      }
    });
  }

  private void handlePing(ChannelHandlerContext ctx) {
    final String responseBody = "Pong";
    final HttpResponse response =
        new DefaultHttpResponse(HTTP_1_1, OK);

    setContentLength(response, responseBody.length());

    ctx.writeAndFlush(response).addListener(future0 -> {
      if (future0.isSuccess()) {
        ctx.writeAndFlush(new DefaultHttpContent(copiedBuffer("Po".getBytes()))).addListener(future1 -> {
          if (future1.isSuccess()) {
            ctx.write(new DefaultHttpContent(copiedBuffer("ng".getBytes())));
            ctx.writeAndFlush(EMPTY_LAST_CONTENT);
          }
        });
      }
    });
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exception caught: {}", cause.getMessage(), cause);
    final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    setContentLength(response, 0);
    ctx.writeAndFlush(response);
  }
}

