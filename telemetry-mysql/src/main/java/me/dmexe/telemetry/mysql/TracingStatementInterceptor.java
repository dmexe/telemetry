package me.dmexe.telemetry.mysql;

import com.mysql.cj.api.MysqlConnection;
import com.mysql.cj.api.jdbc.JdbcConnection;
import com.mysql.cj.api.jdbc.Statement;
import com.mysql.cj.api.jdbc.interceptors.StatementInterceptor;
import com.mysql.cj.api.log.Log;
import com.mysql.cj.api.mysqla.result.Resultset;
import com.mysql.cj.jdbc.PreparedStatement;
import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.Histogram;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class TracingStatementInterceptor implements StatementInterceptor {
  @Override
  public StatementInterceptor init(MysqlConnection conn, Properties props, Log log) {
    return new Handler(conn, MetricsFactory.DEFAULT, log);
  }

  @Override
  public void destroy() {
  }

  @Override
  public boolean executeTopLevelOnly() {
    return true;
  }

  @Override
  public <T extends Resultset> T preProcess(String sql, Statement interceptedStatement) {
    return null;
  }

  @Override
  public <T extends Resultset> T postProcess(
      String sql,
      Statement interceptedStatement,
      T originalResultSet,
      int warningCount,
      boolean noIndexUsed,
      boolean noGoodIndexUsed,
      Exception statementException
  ) {
    return originalResultSet;
  }

  static class Handler implements StatementInterceptor {
    private static final String UNKNOWN_DATABASE = "unknown";
    private static final URI DEFAULT_URI = URI.create("mysql://localhost:3306");

    private final Log log;
    private final MetricsFactory metrics;
    private final String database;
    private final String user;
    private final URI uri;
    private final String serviceName;
    private final Tracer tracer;

    private final ThreadLocal<Histogram.Timer> currentTimer = new ThreadLocal<>();
    private final ThreadLocal<Span> currentSpan = new ThreadLocal<>();

    Handler(MysqlConnection connection, MetricsFactory metrics, Log log) {
      Objects.requireNonNull(connection, "connection cannot be null");
      Objects.requireNonNull(metrics, "metrics cannot be null");
      Objects.requireNonNull(log, "log cannot be null");

      this.tracer = GlobalTracer.get();
      this.log = log;
      this.metrics = metrics;
      this.database = getDatabase(connection);
      this.user = connection.getUser();
      this.uri = getUri(connection);
      this.serviceName = getServiceName(connection);
    }

    @Override
    public StatementInterceptor init(MysqlConnection conn, Properties props, Log log) {
      return this;
    }

    @Override
    public boolean executeTopLevelOnly() {
      return true;
    }

    @Override
    public void destroy() {
    }

    @Override
    public <T extends Resultset> T preProcess(String sql, Statement interceptedStatement) {
      if (interceptedStatement instanceof PreparedStatement) {
        sql = ((PreparedStatement) interceptedStatement).getPreparedSql();
      }

      int spaceIndex = sql.indexOf(' ');
      String name = (spaceIndex == -1) ? sql : sql.substring(0, spaceIndex);
      if (name.equals("/*") || name.equalsIgnoreCase("set")) {
        return null;
      }

      name = name.toLowerCase();
      currentTimer.set(metrics.getLatency(name, database).startTimer());
      metrics.getTotal(name, database).inc();

      ActiveSpan activeSpan = tracer.activeSpan();
      if (activeSpan != null) {
        Span span = tracer
            .buildSpan("sql." + name)
            .asChildOf(activeSpan)
            .startManual();

        Tags.DB_STATEMENT.set(span, sql);
        Tags.DB_USER.set(span, user);
        Tags.DB_INSTANCE.set(span, database);
        Tags.PEER_SERVICE.set(span, serviceName);
        Tags.PEER_HOSTNAME.set(span, uri.getHost());
        Tags.PEER_PORT.set(span, uri.getPort());

        currentSpan.set(span);
      }

      return null;
    }

    @Override
    public <T extends Resultset> T postProcess(
        String sql,
        Statement interceptedStatement,
        T originalResultSet,
        int warningCount,
        boolean noIndexUsed,
        boolean noGoodIndexUsed,
        Exception statementException
    ) {
      try {
        Histogram.Timer latencyTimer = currentTimer.get();
        if (latencyTimer != null) {
          latencyTimer.observeDuration();
        }

        Span span = currentSpan.get();
        if (span != null) {
          if (statementException != null) {
            Tags.ERROR.set(span, true);
            Map<String,String> entries = new HashMap<>();
            entries.put("error.kind", statementException.getClass().getName());
            entries.put("message", statementException.getMessage());
            span.log(entries);
          }
          span.finish();
        }
      } finally {
        currentTimer.remove();
        currentSpan.remove();
      }

      return originalResultSet;
    }

    private String getDatabase(MysqlConnection conn) {
      try {
        if (conn instanceof JdbcConnection) {
          return ((JdbcConnection) conn).getCatalog();
        } else {
          return UNKNOWN_DATABASE;
        }
      } catch (SQLException err) {
        return UNKNOWN_DATABASE;
      }
    }

    private URI getUri(MysqlConnection conn) {
      if (conn instanceof JdbcConnection) {
        String uri = "mysql://" + ((JdbcConnection) conn).getHostPortPair();
        return URI.create(uri);
      } else {
        return DEFAULT_URI;
      }
    }

    private String getServiceName(MysqlConnection conn) {
      String serviceName = conn.getProperties().getProperty("tracingServiceName");
      if (serviceName == null || serviceName.equals("")) {
        serviceName = database.equals(UNKNOWN_DATABASE) ? "mysql" : "mysql-" + database;
      }
      return serviceName;
    }
  }
}
