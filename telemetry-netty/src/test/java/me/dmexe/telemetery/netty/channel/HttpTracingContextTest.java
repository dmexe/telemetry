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
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.prometheus.client.CollectorRegistry;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpTracingContextTest extends TestEnv {
  private CollectorRegistry collectorRegistry;
  private MockTracer tracer;

  @BeforeEach
  void before() {
    collectorRegistry = new CollectorRegistry();
    tracer = new MockTracer(new ThreadLocalActiveSpanSource(), Propagator.TEXT_MAP);
  }

  @Test
  void should_record_get_response_metrics() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        IntStream.range(0, 3).forEach(n -> {
          client.request(get("/ping"));
          final HttpResponse response = client.response();
          assertThat(response).isNotNull();
          assertThat(response.status().code()).isEqualTo(200);
          assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(4);
        });
      }
    }

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_server"))
            .containsExactly(
                "http_server_handled_latency_seconds_bucket{:0,200,GET,+Inf}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.005}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.01}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.025}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.05}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.075}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.1}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.25}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.5}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,0.75}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,1.0}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,10.0}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,2.5}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,5.0}=3.0",
                "http_server_handled_latency_seconds_bucket{:0,200,GET,7.5}=3.0",
                "http_server_handled_latency_seconds_count{:0,200,GET}=3.0",
                "http_server_handled_latency_seconds_sum{:0,200,GET}=0.003",
                "http_server_handled_total{:0,200,GET}=3.0"));

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_client"))
            .containsExactly(
                "http_client_handled_latency_seconds_bucket{:0,200,GET,+Inf}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.005}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.01}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.025}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.05}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.075}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.1}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.25}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.5}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,0.75}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,1.0}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,10.0}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,2.5}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,5.0}=3.0",
                "http_client_handled_latency_seconds_bucket{:0,200,GET,7.5}=3.0",
                "http_client_handled_latency_seconds_count{:0,200,GET}=3.0",
                "http_client_handled_latency_seconds_sum{:0,200,GET}=0.003",
                "http_client_handled_total{:0,200,GET}=3.0"));
  }

  @Test
  void should_record_server_error_metrics() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        client.request(get("/server/error"));
        final HttpResponse response = client.response();
        assertThat(response).isNotNull();
        assertThat(response.status().code()).isEqualTo(500);
        assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(0);
      }
    }

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_server"))
            .containsExactly(
                "http_server_handled_latency_seconds_bucket{:0,500,GET,+Inf}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.005}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.01}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.025}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.05}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.075}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.1}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.25}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.5}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,0.75}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,1.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,10.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,2.5}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,5.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,500,GET,7.5}=1.0",
                "http_server_handled_latency_seconds_count{:0,500,GET}=1.0",
                "http_server_handled_latency_seconds_sum{:0,500,GET}=0.001",
                "http_server_handled_total{:0,500,GET}=1.0"));

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_client"))
            .containsExactly(
                "http_client_handled_latency_seconds_bucket{:0,500,GET,+Inf}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.005}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.01}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.025}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.05}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.075}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.1}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.25}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.5}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.75}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,1.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,10.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,2.5}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,5.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,7.5}=1.0",
                "http_client_handled_latency_seconds_count{:0,500,GET}=1.0",
                "http_client_handled_latency_seconds_sum{:0,500,GET}=0.001",
                "http_client_handled_total{:0,500,GET}=1.0"));
  }

  @Test
  void should_record_get_request_trace() throws Exception {
    final MockSpan root = tracer.buildSpan("root").startManual();

    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        client.request(get("/ping"), root);
        final HttpResponse response = client.response();
        assertThat(response).isNotNull();
        assertThat(response.status().code()).isEqualTo(200);
        assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(4);
      }
    } finally {
      root.finish();
    }

    assertThat(tracer.finishedSpans()).hasSize(3);

    final MockSpan serverSpan = getServerSpan();
    assertThat(serverSpan.tags())
        .containsEntry("component", "netty")
        .containsEntry("span.kind", "server")
        .containsEntry("http.content_length", 4)
        .containsEntry("http.method", "GET")
        .containsEntry("http.url", "/ping")
        .containsKeys("peer.port", "peer.address");
    assertThat(logEntries(serverSpan))
        .containsExactly("event=ss");

    final MockSpan clientSpan = getClientSpan();
    assertThat(clientSpan.tags())
        .containsEntry("component", "netty")
        .containsEntry("span.kind", "client")
        .containsEntry("http.content_length", 4)
        .containsEntry("http.method", "GET")
        .containsEntry("http.url", "/ping")
        .containsKeys("peer.port", "peer.hostname");
    assertThat(logEntries(clientSpan))
        .containsExactly("event=cr");

    assertThat(root.context().traceId())
        .isEqualTo(clientSpan.context().traceId())
        .isEqualTo(serverSpan.context().traceId());
  }

  @Test
  void should_record_server_error_trace() throws Exception {
    final MockSpan root = tracer.buildSpan("root").startManual();

    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Client client = newClient(srv.address(), collectorRegistry, tracer)) {
        client.request(get("/server/error"), root);
        final HttpResponse response = client.response();
        assertThat(response).isNotNull();
        assertThat(response.status().code()).isEqualTo(500);
        assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(0);
      }
    } finally {
      root.finish();
    }

    assertThat(tracer.finishedSpans()).hasSize(3);

    final MockSpan serverSpan = getServerSpan();
    assertThat(serverSpan.tags())
        .containsEntry("component", "netty")
        .containsEntry("span.kind", "server")
        .containsEntry("http.method", "GET")
        .containsEntry("http.url", "/server/error")
        .containsKeys("peer.port", "peer.address");
    assertThat(logEntries(serverSpan))
        .containsExactly("event=ss");

    final MockSpan clientSpan = getClientSpan();
    assertThat(clientSpan.tags())
        .containsEntry("component", "netty")
        .containsEntry("span.kind", "client")
        .containsEntry("http.method", "GET")
        .containsEntry("http.url", "/server/error")
        .containsKeys("peer.port", "peer.hostname");
    assertThat(logEntries(clientSpan))
        .containsExactly("event=cr");

    assertThat(root.context().traceId())
        .isEqualTo(clientSpan.context().traceId())
        .isEqualTo(serverSpan.context().traceId());
  }

  @Test
  void should_record_server_trace_when_client_closed_connection_after_request() throws Exception {
    try (Server srv = newServer(collectorRegistry, tracer)) {
      try (Socket socket = new Socket(InetAddress.getLocalHost(), srv.address().getPort())) {

        final PrintWriter pw = new PrintWriter(socket.getOutputStream());
        pw.println("GET /sleep/second HTTP/1.1");
        pw.println("Host: example.com");
        pw.println("");
        pw.flush();

        // wait response was received.
        Thread.sleep(300);
      }

      // wait server handler.
      Thread.sleep(1000);
    }

    await().timeout(Duration.TWO_SECONDS).untilAsserted(this::getServerSpan);

    final MockSpan serverSpan = getServerSpan();
    assertThat(serverSpan.tags())
        .containsEntry("http.status_code", 499)
        .containsEntry("error", true);
    assertThat(logEntries(serverSpan))
        .containsExactly(
            "error.kind=java.lang.String",
            "error.message=the client closed the connection before the server answered the request",
            "event=ss");

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_server"))
            .containsExactly(
                "http_server_handled_latency_seconds_bucket{:0,499,GET,+Inf}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.005}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.01}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.025}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.05}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.075}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.1}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.25}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.5}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,0.75}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,1.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,10.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,2.5}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,5.0}=1.0",
                "http_server_handled_latency_seconds_bucket{:0,499,GET,7.5}=1.0",
                "http_server_handled_latency_seconds_count{:0,499,GET}=1.0",
                "http_server_handled_latency_seconds_sum{:0,499,GET}=0.001",
                "http_server_handled_total{:0,499,GET}=1.0"));
  }

  @Test
  void should_record_client_trace_when_server_closed_connection_after_request() throws Exception {
    final MockSpan root = tracer.buildSpan("root").startManual();

    try (MockWebServer mockWebServer = new MockWebServer()) {
      final InetSocketAddress address =
          new InetSocketAddress(mockWebServer.getHostName(), mockWebServer.getPort());
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("content-length", "4")
              .setBody("body")
              .setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

      try (Client client = newClient(address, collectorRegistry, tracer)) {
        client.request(get("/get"), root);
        final HttpResponse response = client.response();
        assertThat(response).isNull();
      }
    } finally {
      root.finish();
    }

    await().timeout(Duration.TWO_SECONDS).untilAsserted(this::getClientSpan);
    final MockSpan clientSpan = getClientSpan();

    assertThat(clientSpan.tags())
        .containsEntry("http.status_code", 500)
        .containsEntry("error", true);
    assertThat(logEntries(clientSpan))
        .containsExactly(
            "error.kind=java.lang.String",
            "error.message=the server closed the connection before the client received the response",
            "event=cr");

    await().timeout(TWO_SECONDS).untilAsserted(() ->
        assertThat(samples(collectorRegistry, "http_client"))
            .containsExactly(
                "http_client_handled_latency_seconds_bucket{:0,500,GET,+Inf}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.005}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.01}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.025}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.05}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.075}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.1}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.25}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.5}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,0.75}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,1.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,10.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,2.5}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,5.0}=1.0",
                "http_client_handled_latency_seconds_bucket{:0,500,GET,7.5}=1.0",
                "http_client_handled_latency_seconds_count{:0,500,GET}=1.0",
                "http_client_handled_latency_seconds_sum{:0,500,GET}=0.001",
                "http_client_handled_total{:0,500,GET}=1.0"));
  }

  private static List<String> logEntries(MockSpan mockSpan) {
    return mockSpan.logEntries().stream()
        .flatMap(it -> it
            .fields()
            .entrySet().stream()
        )
        .map(it -> it.getKey() + "=" + it.getValue())
        .sorted()
        .collect(Collectors.toList());
  }

  private MockSpan getServerSpan() {
   return tracer.finishedSpans().stream()
       .filter(it -> it.tags().containsValue("server"))
       .findFirst()
       .orElseThrow(() -> new RuntimeException("cannot found server span"));
  }

  private MockSpan getClientSpan() {
    return tracer.finishedSpans().stream()
        .filter(it -> it.tags().containsValue("client"))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("cannot found client span"));
  }

  private static FullHttpRequest get(String uri) {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
  }
}
