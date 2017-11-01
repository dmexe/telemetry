package me.dmexe.telemetry.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;

class MetricsTest extends TestEnv {

  @Test
  void should_record_queries() throws SQLException {
    int insertTotal = sample("mysql_started_total", "insert");
    int selectTotal = sample("mysql_started_total", "select");

    try (Connection conn = dataSource().getConnection()) {
      PreparedStatement insert = conn.prepareStatement("INSERT INTO pages (id, created_at) VALUES(?,?)");
      insert.setInt(1, 1);
      insert.setTimestamp(2, Timestamp.from(Instant.now()));
      insert.execute();
      insert.close();

      try(Statement query = conn.createStatement();
          ResultSet rs = query.executeQuery("SELECT * FROM pages")) {
        rs.first();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }
    }

    await().timeout(Duration.TWO_SECONDS).untilAsserted(() -> {
      assertThat(sample("mysql_started_total", "insert")).isEqualTo(insertTotal + 1);
      assertThat(sample("mysql_started_total", "select")).isEqualTo(selectTotal + 1);

      assertThat(sample("mysql_latency_seconds_count", "insert")).isEqualTo(insertTotal + 1);
      assertThat(sample("mysql_latency_seconds_count", "select")).isEqualTo(selectTotal + 1);
    });
  }

  @Test
  void should_record_batch_queries() throws SQLException {
    int insertTotal = sample("mysql_started_total", "insert");
    int selectTotal = sample("mysql_started_total", "select");

    try (Connection conn = dataSource().getConnection()) {
      PreparedStatement insert = conn.prepareStatement("INSERT INTO pages (id, created_at) VALUES(?,?)");

      insert.setInt(1, 1);
      insert.setTimestamp(2, Timestamp.from(Instant.now()));
      insert.addBatch();

      insert.setInt(1, 2);
      insert.setTimestamp(2, Timestamp.from(Instant.now()));
      insert.addBatch();

      insert.executeBatch();
      insert.close();

      try(Statement query = conn.createStatement();
          ResultSet rs = query.executeQuery("SELECT * FROM pages")) {
        rs.last();
        assertThat(rs.getInt(1)).isEqualTo(2);
      }
    }

    await().timeout(Duration.TWO_SECONDS).untilAsserted(() -> {
      assertThat(sample("mysql_started_total", "insert")).isEqualTo(insertTotal + 2);
      assertThat(sample("mysql_started_total", "select")).isEqualTo(selectTotal + 1);

      assertThat(sample("mysql_latency_seconds_count", "insert")).isEqualTo(insertTotal + 2);
      assertThat(sample("mysql_latency_seconds_count", "select")).isEqualTo(selectTotal + 1);
    });
  }

  private int sample(String metricName, String name) {
    Double value = collectorRegistry().getSampleValue(
        metricName,
        new String[]{"name", "database"},
        new String[]{name, "telemetry"});
    return value == null ? 0 : value.intValue();
  }
}
