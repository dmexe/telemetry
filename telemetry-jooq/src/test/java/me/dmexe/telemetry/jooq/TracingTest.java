package me.dmexe.telemetry.jooq;

import static me.dmexe.telemetry.jooq.tables.Pages.PAGES;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.ActiveSpan;
import java.sql.Timestamp;
import me.dmexe.telemetry.jooq.tables.records.PagesRecord;
import org.jooq.Cursor;
import org.junit.jupiter.api.Test;

class TracingTest extends TestEnv {

  @Test
  void should_record_writes() {
    newSpan(() ->
        db().insertInto(PAGES).set(record(1)).execute());

    assertThat(tracer().finishedSpans()).hasSize(2);
    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.WRITE");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "insert into `pages` (`id`, `created_at`, `updated_at`) values (?, ?, ?)")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");

    newSpan(() ->
      db().update(PAGES).set(PAGES.UPDATED_AT, timestamp()).where(PAGES.ID.eq(1)).execute());

    assertThat(tracer().finishedSpans()).hasSize(2);
    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.WRITE");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "update `pages` set `pages`.`updated_at` = ? where `pages`.`id` = ?")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");

    newSpan(() ->
      db().deleteFrom(PAGES).where(PAGES.ID.eq(1)).execute());

    assertThat(tracer().finishedSpans()).hasSize(2);
    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.WRITE");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "delete from `pages` where `pages`.`id` = ?")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");
  }

  @Test
  void should_record_reads() {
    db()
        .batchInsert(
            record(1),
            record(2),
            record(3))
        .execute();

    newSpan(() ->
      db().selectFrom(PAGES).where(PAGES.ID.eq(1)).execute());

    assertThat(tracer().finishedSpans()).hasSize(2);
    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.READ");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "select `pages`.`id`, `pages`.`created_at`, `pages`.`updated_at` from `pages` where `pages`.`id` = ?")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");

    newSpan(() -> {
      try (Cursor<PagesRecord> cursor = db().selectFrom(PAGES).fetchLazy()) {
        for (PagesRecord rec : cursor) {
          assertThat(rec).isNotNull();
        }
      }
    });

    assertThat(tracer().finishedSpans()).hasSize(2);
    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.READ");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "select `pages`.`id`, `pages`.`created_at`, `pages`.`updated_at` from `pages`")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");
  }

  @Test
  void should_record_batch_inserts() {
    newSpan(() -> db()
        .batchInsert(
            record(1),
            record(2),
            record(3))
        .execute());

    assertThat(tracer().finishedSpans()).hasSize(5);

    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.WRITE");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "insert into `pages` (`id`, `created_at`, `updated_at`) values (?, ?, ?)")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");

    assertThat(tracer().finishedSpans().get(3).operationName())
        .isEqualTo("sql.BATCH");
    assertThat(tracer().finishedSpans().get(3).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "insert into `pages` (`id`, `created_at`, `updated_at`) values (?, ?, ?)")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");
  }

  @Test
  void should_record_batch_statements() {
    newSpan(() -> db()
        .batch(
            db()
                .insertInto(PAGES, PAGES.ID, PAGES.CREATED_AT, PAGES.UPDATED_AT)
                .values((Integer)null, (Timestamp) null, (Timestamp)null))
        .bind(1, timestamp(), timestamp())
        .bind(2, timestamp(), timestamp())
        .bind(3, timestamp(), timestamp())
        .execute());

    assertThat(tracer().finishedSpans()).hasSize(2);

    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.BATCH");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("component", "jooq")
        .containsEntry("db.statement", "insert into `pages` (`id`, `created_at`, `updated_at`) values (?, ?, ?)")
        .containsEntry("db.type", "MySQL")
        .containsEntry("span.kind", "client");
  }

  private void newSpan(Runnable runnable) {
    tracer().reset();
    try (ActiveSpan ignored = tracer().buildSpan("root").startActive()) {
      runnable.run();
    }
  }
}
