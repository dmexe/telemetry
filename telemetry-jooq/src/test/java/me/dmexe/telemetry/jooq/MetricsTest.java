package me.dmexe.telemetry.jooq;

import static me.dmexe.telemetry.jooq.tables.Pages.PAGES;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import me.dmexe.telemetry.jooq.tables.records.PagesRecord;
import org.jooq.Cursor;
import org.junit.jupiter.api.Test;

class MetricsTest extends TestEnv {

  @Test
  void should_record_writes() {
    db().insertInto(PAGES).set(record(1)).execute();

    assertThat(sample("jooq_started_total", "WRITE")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_count", "WRITE")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_sum", "WRITE")).isEqualTo(0);

    db().update(PAGES).set(PAGES.UPDATED_AT, Timestamp.from(Instant.now())).where(PAGES.ID.eq(1)).execute();

    assertThat(sample("jooq_started_total", "WRITE")).isEqualTo(2);
    assertThat(sample("jooq_latency_seconds_count", "WRITE")).isEqualTo(2);
    assertThat(sample("jooq_latency_seconds_sum", "WRITE")).isEqualTo(0);

    db().deleteFrom(PAGES).where(PAGES.ID.eq(1)).execute();

    assertThat(sample("jooq_started_total", "WRITE")).isEqualTo(3);
    assertThat(sample("jooq_latency_seconds_count", "WRITE")).isEqualTo(3);
    assertThat(sample("jooq_latency_seconds_sum", "WRITE")).isEqualTo(0);
  }

  @Test
  void should_record_reads() {
    db()
        .batchInsert(
            record(1),
            record(2),
            record(3))
        .execute();

    db().selectFrom(PAGES).where(PAGES.ID.eq(1)).execute();

    assertThat(sample("jooq_started_total", "READ")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_count", "READ")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_sum", "READ")).isEqualTo(0);

    try (Cursor<PagesRecord> cursor = db().selectFrom(PAGES).fetchLazy()) {
      for (PagesRecord rec : cursor) {
        assertThat(rec).isNotNull();
      }
    }

    assertThat(sample("jooq_started_total", "READ")).isEqualTo(2);
    assertThat(sample("jooq_latency_seconds_count", "READ")).isEqualTo(2);
    assertThat(sample("jooq_latency_seconds_sum", "READ")).isEqualTo(0);
  }

  @Test
  void should_record_batch_inserts() {
    db()
        .batchInsert(
            record(1),
            record(2),
            record(3))
        .execute();

    assertThat(sample("jooq_started_total", "WRITE")).isEqualTo(3);
    assertThat(sample("jooq_started_total", "BATCH")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_count", "BATCH")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_sum", "BATCH")).isEqualTo(0);
  }

  @Test
  void should_record_batch_statement() {
    Timestamp time = Timestamp.from(Instant.now());

    db()
        .batch(
            db()
                .insertInto(PAGES, PAGES.ID, PAGES.CREATED_AT, PAGES.UPDATED_AT)
                .values((Integer)null, (Timestamp) null, (Timestamp) null))
        .bind(1, time, time)
        .bind(2, time, time)
        .bind(3, time, time)
        .execute();

    assertThat(sample("jooq_started_total", "WRITE")).isEqualTo(-1);
    assertThat(sample("jooq_started_total", "BATCH")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_count", "BATCH")).isEqualTo(1);
    assertThat(sample("jooq_latency_seconds_sum", "BATCH")).isEqualTo(0);
  }

  private long sample(String name, String op) {
    Double value = collectorRegistry().getSampleValue(name, new String[]{"sql_op"}, new String[]{op});
    return value == null ? -1 : value.longValue();
  }
}
