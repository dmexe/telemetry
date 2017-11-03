package me.dmexe.telemetry.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentracing.ActiveSpan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;

class TracingTest extends TestEnv {

  @Test
  void should_record_select_query() throws Throwable {
    newSpan(() -> {
      try (Connection conn = dataSource().getConnection();
           Statement query = conn.createStatement();
           ResultSet rs = query.executeQuery("SELECT * FROM pages")) {
        assertThat(rs).isNotNull();
      }
    });

    assertThat(tracer().finishedSpans()).hasSize(2);

    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.select");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("db.instance", "telemetry")
        .containsEntry("db.statement", "SELECT * FROM pages")
        .containsEntry("db.user", "root")
        .containsEntry("peer.hostname", "localhost")
        .containsEntry("peer.port", 3306)
        .containsEntry("peer.service", "MySql");
  }

  @Test
  void should_record_insert() throws Throwable {
    String sql = "insert into pages (id, created_at) values(?,?)";

    newSpan(() -> {
      try (Connection conn = dataSource().getConnection();
           PreparedStatement insert = conn.prepareStatement(sql)) {
        insert.setInt(1, 1);
        insert.setTimestamp(2, timestamp());
        insert.execute();
      }
    });

    assertThat(tracer().finishedSpans()).hasSize(2);

    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.insert");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("db.instance", "telemetry")
        .containsEntry("db.statement", sql)
        .containsEntry("db.user", "root")
        .containsEntry("peer.hostname", "localhost")
        .containsEntry("peer.port", 3306)
        .containsEntry("peer.service", "MySql");
  }

  @Test
  void should_record_exceptions() throws Throwable {
    String sql = "select from pages";

    newSpan(() -> assertThatThrownBy(() -> {
          try (Connection conn = dataSource().getConnection();
               Statement query = conn.createStatement();
               ResultSet rs = query.executeQuery(sql)) {
            assertThat(rs).isNotNull();
          }
        }).isInstanceOf(SQLSyntaxErrorException.class)
    );

    assertThat(tracer().finishedSpans()).hasSize(2);

    assertThat(tracer().finishedSpans().get(0).operationName())
        .isEqualTo("sql.select");
    assertThat(tracer().finishedSpans().get(0).tags())
        .containsEntry("db.instance", "telemetry")
        .containsEntry("db.statement", sql)
        .containsEntry("db.user", "root")
        .containsEntry("peer.hostname", "localhost")
        .containsEntry("peer.port", 3306)
        .containsEntry("peer.service", "MySql")
        .containsEntry("error", true);
  }
  private void newSpan(ThrowingRunnable runnable) throws Throwable {
    tracer().reset();
    try (ActiveSpan ignored = tracer().buildSpan("root").startActive()) {
      runnable.run();
    }
  }
}
