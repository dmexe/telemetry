package me.dmexe.telemetry.mysql;

import static me.dmexe.telemetry.mysql.CreateTables.databasePassword;
import static me.dmexe.telemetry.mysql.CreateTables.databaseUrl;
import static me.dmexe.telemetry.mysql.CreateTables.databaseUser;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.prometheus.client.CollectorRegistry;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class TestEnv {
  private static MockTracer tracer;
  private static CollectorRegistry collectorRegistry;

  private HikariDataSource dataSource;

  static {
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());
    collectorRegistry = CollectorRegistry.defaultRegistry;

    GlobalTracer.register(tracer);
  }

  @BeforeEach
  void setup() throws Exception {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(databaseUrl());
    cfg.setUsername(databaseUser());
    cfg.setPassword(databasePassword());
    cfg.addDataSourceProperty("statementInterceptors", TracingStatementInterceptor.class.getName());

    dataSource = new HikariDataSource(cfg);

    try(Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("truncate `pages`");
    }
  }

  @AfterEach
  void after() {
    if (dataSource != null) {
      dataSource.close();
      dataSource = null;
    }
  }

  CollectorRegistry collectorRegistry() {
    return collectorRegistry;
  }

  Timestamp timestamp() {
    return Timestamp.from(Instant.now());
  }

  MockTracer tracer() {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    return tracer;
  }

  DataSource dataSource() {
    Objects.requireNonNull(dataSource, "dataSource cannot be null");
    return dataSource;
  }
}
