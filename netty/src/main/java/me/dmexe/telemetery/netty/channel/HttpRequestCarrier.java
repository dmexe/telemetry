package me.dmexe.telemetery.netty.channel;

import io.netty.handler.codec.http.HttpRequest;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

class HttpRequestCarrier implements TextMap {
  private final HttpRequest request;

  HttpRequestCarrier(HttpRequest request) {
    Objects.requireNonNull(request, "request cannot be null");
    this.request = request;
  }

  @Override
  public Iterator<Entry<String, String>> iterator() {
    return request.headers().iteratorAsString();
  }

  @Override
  public void put(String key, String value) {
    System.out.println("key=" + key + " value=" + value);
    request.headers().add(key, value);
  }
}
