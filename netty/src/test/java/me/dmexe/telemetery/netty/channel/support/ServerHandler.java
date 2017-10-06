package me.dmexe.telemetery.netty.channel.support;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.parseLine;
import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
      } else {
        handleNotFound(ctx);
      }
    } else {
      super.channelRead(ctx, msg);
    }
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
    });
    ctx.writeAndFlush(EMPTY_LAST_CONTENT);
  }

  private void handlePing(ChannelHandlerContext ctx) {
    final String responseBody = "Pong";
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HTTP_1_1, OK, copiedBuffer(responseBody.getBytes()));

    setContentLength(response, responseBody.length());
    ctx.writeAndFlush(response);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exception caught: {}", cause.getMessage(), cause);
    ctx.writeAndFlush(
        new DefaultFullHttpResponse(
            HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            copiedBuffer(cause.getMessage().getBytes())
        ));
  }
}

