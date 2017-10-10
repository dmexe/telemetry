package me.dmexe.telemetery.netty.alloc;

import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PooledByteBufAllocatorCollector extends Collector {
  private static final String DIRECT_ARENA = "direct";
  private static final String HEAP_ARENA = "heap";
  private static final String PREFIX = "netty_alloc_";

  private static final Map<String,WeakReference<PooledByteBufAllocator>> refs =
      new ConcurrentHashMap<>();

  /**
   * Register and collect metrics for a {@link PooledByteBufAllocator} alloc.
   *
   * @param name the allocator identifier
   * @param alloc the allocator instance
   */
  public static void add(String name, PooledByteBufAllocator alloc) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(alloc, "alloc cannot be null");
    refs.putIfAbsent(name, new WeakReference<>(alloc));
  }

  /**
   * Check that given name already registered in the watch list.
   *
   * @param name allocator name.
   * @return true al allocator already registered.
   */
  public static boolean contains(String name) {
    return refs.containsKey(name);
  }

  /**
   * Remove a {@link PooledByteBufAllocator} from the watch list.
   *
   * @param  name a name of {@link PooledByteBufAllocator}
   */
  public static void remove(String name) {
    refs.remove(name);
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final GaugeMetricFamily arenaNumActiveAllocations =
        gauge("num_active_allocations", "The number of currently active allocations.",
            "pool", "arena");
    final GaugeMetricFamily arenaNumAllocations =
        gauge("num_allocations", "The number of allocations done via the arena",
            "pool", "arena");
    final GaugeMetricFamily arenaNumDeallocations =
        gauge("num_deallocations", "The number of deallocations done via the arena.",
            "pool", "arena");

    refs.forEach((name, ref) -> {
      final PooledByteBufAllocator alloc = ref.get();
      if (alloc == null) {
        return;
      }

      final List<String> heapLabelValues = new ArrayList<>(2);
      heapLabelValues.add(name);
      heapLabelValues.add(HEAP_ARENA);

      final List<String> directLabelValues = new ArrayList<>(2);
      directLabelValues.add(name);
      directLabelValues.add(DIRECT_ARENA);

      long value;

      value = alloc.metric()
          .heapArenas().stream()
          .mapToLong(PoolArenaMetric::numActiveAllocations).sum();
      arenaNumActiveAllocations.addMetric(heapLabelValues, value);

      value = alloc.metric()
          .heapArenas().stream()
          .mapToLong(PoolArenaMetric::numAllocations).sum();
      arenaNumAllocations.addMetric(heapLabelValues, value);

      value = alloc.metric()
          .heapArenas().stream()
          .mapToLong(PoolArenaMetric::numDeallocations).sum();
      arenaNumDeallocations.addMetric(heapLabelValues, value);

      value = alloc.metric()
          .directArenas().stream()
          .mapToLong(PoolArenaMetric::numActiveAllocations).sum();
      arenaNumActiveAllocations.addMetric(directLabelValues, value);

      value = alloc.metric()
          .directArenas().stream()
          .mapToLong(PoolArenaMetric::numAllocations).sum();
      arenaNumAllocations.addMetric(directLabelValues, value);

      value = alloc.metric()
          .directArenas().stream()
          .mapToLong(PoolArenaMetric::numDeallocations).sum();
      arenaNumDeallocations.addMetric(directLabelValues, value);
    });

    final List<MetricFamilySamples> samples = new LinkedList<>();

    samples.add(arenaNumActiveAllocations);
    samples.add(arenaNumAllocations);
    samples.add(arenaNumDeallocations);

    return samples;
  }

  private GaugeMetricFamily gauge(String name, String desc, String... labels) {
    return new GaugeMetricFamily(PREFIX + name, desc, Arrays.asList(labels));
  }
}
