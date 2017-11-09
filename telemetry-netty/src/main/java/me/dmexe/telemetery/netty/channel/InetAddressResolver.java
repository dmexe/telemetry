package me.dmexe.telemetery.netty.channel;

import static io.opentracing.tag.Tags.PEER_HOSTNAME;
import static me.dmexe.telemetery.netty.channel.NettyConstants.LOCAL_ADDRESS;
import static me.dmexe.telemetery.netty.channel.NettyConstants.PEER_ADDRESS;

import io.opentracing.Span;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

class InetAddressResolver {
  private final InetSocketAddress inetSocketAddress;

  InetAddressResolver(SocketAddress socketAddress) {
    if (socketAddress != null && socketAddress instanceof InetSocketAddress) {
      this.inetSocketAddress = (InetSocketAddress) socketAddress;
    } else {
      this.inetSocketAddress = null;
    }
  }

  void setPeerAddress(Span span) {
    if (inetSocketAddress == null) {
      return;
    }

    if (span == null) {
      return;
    }

    final InetAddress inetAddress = inetSocketAddress.getAddress();

    if (inetSocketAddress.isUnresolved()
        || inetAddress == null
        || inetAddress.getHostAddress() == null) {
      PEER_HOSTNAME.set(
          span,
          inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort());
    } else {
      PEER_ADDRESS.set(
          span,
          inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort());
    }
  }

  void setLocalAddress(Span span) {
    if (inetSocketAddress == null || inetSocketAddress.isUnresolved()) {
      return;
    }

    if (span == null) {
      return;
    }

    final InetAddress inetAddress = inetSocketAddress.getAddress();

    if (inetAddress != null && inetAddress.getHostAddress() != null) {
      LOCAL_ADDRESS.set(
          span,
          inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort());
    }
  }
}
