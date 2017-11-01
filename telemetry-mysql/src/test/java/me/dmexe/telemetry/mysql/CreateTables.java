package me.dmexe.telemetry.mysql;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateTables {
  public static void main(String... args) throws SQLException {
    final InputStream is = CreateTables.class.getResourceAsStream("/db/migrate/V1_CreateTables.sql");
    Objects.requireNonNull(is, "is cannot be null");

    final String sql = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    final String url = databaseUrl();
    try (Connection conn = DriverManager.getConnection(url, databaseUser(), databasePassword());
         Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  static String databaseUser() {
    String user = System.getenv("DATABASE_USER");
    if (user != null) {
      return user;
    } else {
      return "root";
    }
  }

  static String databasePassword() {
    String password = System.getenv("DATABASE_PASSWORD");
    if (password != null) {
      return password;
    } else {
      return "root";
    }
  }

  static String databaseUrl() {
    String host = System.getenv("DATABASE_HOST");
    if (host == null) {
      host = "localhost";
    }

    int port = 3306;
    String portString = System.getenv("DATABASE_PORT");
    if (portString != null) {
      port = Integer.parseInt(portString);
    }

    return String.format("jdbc:mysql://%s:%d/telemetry?createDatabaseIfNotExist=true", host, port);
  }
}
