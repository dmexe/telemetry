package me.dmexe.telemetery.netty.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.TWO_SECONDS;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.prometheus.client.CollectorRegistry;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NettyChannelTracingContextTest extends TestEnv {

  private CollectorRegistry collectorRegistry;
  private MockTracer tracer;

  @BeforeEach
  void before() {
    collectorRegistry = new CollectorRegistry();
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());
  }

  @Test
  void should_record_get_response_metrics() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        IntStream.range(0, 3).forEach(n -> {
          client.request(get("/ping"));
          final HttpResponse response = client.response();
          assertThat(response.status().code()).isEqualTo(200);
          assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(4);
        });
      }
    }

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "netty_client"))
            .containsExactly(
                "netty_client_connection_duration_seconds_count{:0}=1.0",
                "netty_client_connection_duration_seconds_sum{:0}=0.001",
                "netty_client_connections_count{:0}=0.0",
                "netty_client_connects_total{:0,active}=1.0",
                "netty_client_connects_total{:0,inactive}=1.0",
                "netty_client_received_bytes{:0}=126.0",
                "netty_client_send_bytes{:0}=66.0"));

    assertThat(samples(collectorRegistry, "netty_server"))
        .containsExactly(
            "netty_server_connection_duration_seconds_count{:0}=1.0",
            "netty_server_connection_duration_seconds_sum{:0}=0.001",
            "netty_server_connections_count{:0}=0.0",
            "netty_server_connects_total{:0,active}=1.0",
            "netty_server_connects_total{:0,inactive}=1.0",
            "netty_server_received_bytes{:0}=66.0",
            "netty_server_send_bytes{:0}=126.0");
  }

  @Test
  void should_record_file_region_response_metrics() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        IntStream.range(0, 3).forEach(n -> {
          client.request(get("/file"));
          final HttpResponse response = client.response();
          assertThat(response.status().code()).isEqualTo(200);
          assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(8192);
        });
      }
    }

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "netty_client"))
            .containsExactly(
                "netty_client_connection_duration_seconds_count{:0}=1.0",
                "netty_client_connection_duration_seconds_sum{:0}=0.001",
                "netty_client_connections_count{:0}=0.0",
                "netty_client_connects_total{:0,active}=1.0",
                "netty_client_connects_total{:0,inactive}=1.0",
                "netty_client_received_bytes{:0}=24699.0",
                "netty_client_send_bytes{:0}=66.0"));

    assertThat(samples(collectorRegistry, "netty_server"))
        .containsExactly(
            "netty_server_connection_duration_seconds_count{:0}=1.0",
            "netty_server_connection_duration_seconds_sum{:0}=0.001",
            "netty_server_connections_count{:0}=0.0",
            "netty_server_connects_total{:0,active}=1.0",
            "netty_server_connects_total{:0,inactive}=1.0",
            "netty_server_received_bytes{:0}=66.0",
            "netty_server_send_bytes{:0}=24699.0");
  }

  @Test
  void should_record_server_error_metrics() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        client.request(get("/server/error"));
        final HttpResponse response = client.response();
        assertThat(response.status().code()).isEqualTo(500);
        assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(0);
      }
    }

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "netty_client"))
            .containsExactly(
                "netty_client_connection_duration_seconds_count{:0}=1.0",
                "netty_client_connection_duration_seconds_sum{:0}=0.001",
                "netty_client_connections_count{:0}=0.0",
                "netty_client_connects_total{:0,active}=1.0",
                "netty_client_connects_total{:0,inactive}=1.0",
                "netty_client_received_bytes{:0}=57.0",
                "netty_client_send_bytes{:0}=30.0"));

    assertThat(samples(collectorRegistry, "netty_server"))
        .containsExactly(
            "netty_server_connection_duration_seconds_count{:0}=1.0",
            "netty_server_connection_duration_seconds_sum{:0}=0.001",
            "netty_server_connections_count{:0}=0.0",
            "netty_server_connects_total{:0,active}=1.0",
            "netty_server_connects_total{:0,inactive}=1.0",
            "netty_server_received_bytes{:0}=30.0",
            "netty_server_send_bytes{:0}=57.0");
  }

  private static FullHttpRequest get(String uri) {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
  }
}
