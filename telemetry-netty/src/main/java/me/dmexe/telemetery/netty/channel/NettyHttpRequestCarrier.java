package me.dmexe.telemetery.netty.channel;

import io.netty.handler.codec.http.HttpRequest;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

class NettyHttpRequestCarrier implements TextMap {
  private final HttpRequest request;

  NettyHttpRequestCarrier(HttpRequest request) {
    Objects.requireNonNull(request, "request cannot be null");
    this.request = request;
  }

  @Override
  @NotNull
  public Iterator<Entry<String, String>> iterator() {
    return request.headers().iteratorAsString();
  }

  @Override
  public void put(String key, String value) {
    request.headers().add(key, value);
  }
}
