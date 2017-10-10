package me.dmexe.telemetry.jooq;

import static me.dmexe.telemetry.jooq.CreateTables.databasePassword;
import static me.dmexe.telemetry.jooq.CreateTables.databaseUrl;
import static me.dmexe.telemetry.jooq.CreateTables.databaseUser;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import io.prometheus.client.CollectorRegistry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import me.dmexe.telemetry.jooq.tables.records.PagesRecord;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class TestEnv {
  private MockTracer tracer;
  private Configuration cfg;
  private Connection connection;
  private DSLContext db;
  private CollectorRegistry collectorRegistry;

  @BeforeEach
  void setup() throws Exception {
    collectorRegistry = new CollectorRegistry();
    connection = DriverManager.getConnection(databaseUrl(), databaseUser(), databasePassword());
    tracer = new MockTracer(new ThreadLocalActiveSpanSource());

    cfg = new DefaultConfiguration();
    cfg
        .set(SQLDialect.MYSQL)
        .set(connection)
        .set(new DefaultJooqTracingFactory().collectorRegistry(collectorRegistry).tracer(tracer).createListener())
        .settings().withRenderSchema(false);

    db = new DefaultDSLContext(cfg);
    db.execute("truncate `pages`");
  }

  @AfterEach
  void after() throws Exception {
    if (connection != null) {
      connection.close();
      connection = null;
    }

    if (db != null) {
      db.close();
      db = null;
    }
  }

  CollectorRegistry collectorRegistry() {
    return collectorRegistry;
  }

  Timestamp timestamp() {
    return Timestamp.from(Instant.now());
  }

  PagesRecord record(int id) {
    final PagesRecord page = new PagesRecord();
    page.setId(id);
    page.setCreatedAt(Timestamp.from(Instant.now()));
    page.setUpdatedAt(Timestamp.from(Instant.now()));
    return page;
  }

  MockTracer tracer() {
    Objects.requireNonNull(tracer, "tracer cannot be null");
    return tracer;
  }

  DSLContext db() {
    Objects.requireNonNull(db, "db cannot be null");
    return db;
  }
}
