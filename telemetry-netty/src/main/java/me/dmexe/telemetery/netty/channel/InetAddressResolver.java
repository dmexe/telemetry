package me.dmexe.telemetery.netty.channel;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

class InetAddressResolver {
  private final InetSocketAddress inetSocketAddress;

  InetAddressResolver(SocketAddress socketAddress) {
    if (socketAddress != null && socketAddress instanceof InetSocketAddress) {
      this.inetSocketAddress = (InetSocketAddress) socketAddress;
    } else {
      this.inetSocketAddress = null;
    }
  }

  void set(Span span) {
    if (inetSocketAddress == null) {
      return;
    }

    if (span == null) {
      return;
    }

    Tags.PEER_PORT.set(span, inetSocketAddress.getPort());
    final InetAddress inetAddress = inetSocketAddress.getAddress();

    if (inetAddress instanceof Inet4Address) {
      final Inet4Address inet4Address = (Inet4Address) inetAddress;
      Tags.PEER_HOST_IPV4.set(span, ByteBuffer.wrap(inet4Address.getAddress()).getInt());
    } else if (inetAddress instanceof Inet6Address) {
      final Inet6Address inet6Address = (Inet6Address) inetAddress;
      Tags.PEER_HOST_IPV6.set(span, inet6Address.getHostAddress());
    } else {
      Tags.PEER_HOSTNAME.set(span, inetAddress.getHostName());
    }
  }
}
