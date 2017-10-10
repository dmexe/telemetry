package me.dmexe.telemetery.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import java.util.Objects;

class NettyChannelTracingHandler extends ChannelDuplexHandler {
  private final NettyChannelTracingContext stats;

  NettyChannelTracingHandler(NettyChannelTracingContext stats) {
    Objects.requireNonNull(stats, "stats cannot be null");
    this.stats = stats;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    stats.channelActive();
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    stats.channelInactive();
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    stats.exceptionCaught();
    super.exceptionCaught(ctx, cause);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {

    if (msg instanceof ByteBuf) {
      final ByteBuf bytes = (ByteBuf)msg;
      stats.write(bytes.readableBytes());
    } else if (msg instanceof FileRegion) {
      final FileRegion file = (FileRegion)msg;
      stats.write(file.count());
    }

    super.write(ctx, msg, promise);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      final ByteBuf bytes = (ByteBuf)msg;
      stats.read(bytes.readableBytes());
    }

    super.channelRead(ctx, msg);
  }
}
