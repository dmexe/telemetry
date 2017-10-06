package me.dmexe.telemetery.netty.alloc;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCounted;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PooledByteBufAllocatorCollectorTest {
  private CollectorRegistry collectorRegistry;
  private PooledByteBufAllocator alloc;

  @BeforeEach
  void before() {
    collectorRegistry = new CollectorRegistry();
    alloc = new PooledByteBufAllocator();

    PooledByteBufAllocatorCollector.remove("test");
    PooledByteBufAllocatorCollector.add("test", alloc);

    new PooledByteBufAllocatorCollector().register(collectorRegistry);
  }

  @Test
  void should_collect_heap_metrics() {
    final List<ByteBuf> heapBuffers = IntStream.range(0, 10)
        .mapToObj(n -> alloc.heapBuffer(8 * 1024))
        .collect(Collectors.toList());

    assertThat(samples(collectorRegistry))
        .contains(
            "netty_alloc_num_active_allocations{test,heap}=10.0",
            "netty_alloc_num_allocations{test,heap}=10.0",
            "netty_alloc_num_deallocations{test,heap}=0.0");

    heapBuffers.forEach(ReferenceCounted::release);
  }

  @Test
  void should_collect_direct_metrics() {
    final List<ByteBuf> directBuffers = IntStream.range(0, 10)
        .mapToObj(n -> alloc.directBuffer(8 * 1024))
        .collect(Collectors.toList());

    assertThat(samples(collectorRegistry))
        .contains(
            "netty_alloc_num_active_allocations{test,direct}=10.0",
            "netty_alloc_num_allocations{test,direct}=10.0",
            "netty_alloc_num_deallocations{test,direct}=0.0");

    directBuffers.forEach(ReferenceCounted::release);
  }

  private static List<String> samples(CollectorRegistry collectorRegistry) {
    return Collections.list(collectorRegistry.metricFamilySamples()).stream()
        .flatMap(family ->
            family.samples.stream().map(sample ->
                sample.name + "{" + String.join(",", sample.labelValues) + "}=" + sample.value
            )
        )
        .sorted()
        .collect(Collectors.toList());
  }
}
