package com.kingpixel.ultradaycare.database.repository.impl;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.UserRepository;
import com.kingpixel.ultradaycare.models.User;
import org.jspecify.annotations.Nullable;

import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SqlUserRepository handles User persistence for SQL databases (H2, SQLite, MySQL, MariaDB, PostgreSQL).
 *
 * @author Carlos Varas Alonso
 */
public class SqlUserRepository implements UserRepository {
  private final DataBaseConfig config;
  private final String jdbcUrl;
  private final String username;
  private final String password;

  public SqlUserRepository(DataBaseConfig config) {
    this.config = config;
    this.jdbcUrl = buildJdbcUrl(config);
    this.username = config.getUser() != null ? config.getUser() : "";
    this.password = config.getPassword() != null ? config.getPassword() : "";
    initTable();
  }

  private String buildJdbcUrl(DataBaseConfig config) {
    if (config.getUrl() != null && !config.getUrl().isEmpty()) {
      return config.getUrl();
    }
    String dbName = config.getDatabase() != null && !config.getDatabase().isEmpty() ? config.getDatabase() : "ultradaycare";
    String type = config.getType() != null ? config.getType().name().toLowerCase() : "sqlite";
    switch (type) {
      case "h2":
        return "jdbc:h2:" + UltraDaycare.getPath().resolve("data").resolve("database").toAbsolutePath();
      case "mysql":
        return "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
      case "mariadb":
        return "jdbc:mariadb://localhost:3306/" + dbName;
      case "sqlite":
      default:
        return "jdbc:sqlite:" + UltraDaycare.getPath().resolve("data").resolve("database.db").toAbsolutePath();
    }
  }

  private Connection getConnection() throws SQLException {
    if (username.isEmpty()) {
      return DriverManager.getConnection(jdbcUrl);
    }
    return DriverManager.getConnection(jdbcUrl, username, password);
  }

  private void initTable() {
    String createSql = "CREATE TABLE IF NOT EXISTS ultradaycare_users (" +
      "player_uuid VARCHAR(36) PRIMARY KEY, " +
      "player_name VARCHAR(64), " +
      "data TEXT, " +
      "updated_at BIGINT)";
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(createSql);
      UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "SQL Table 'ultradaycare_users' initialized successfully.");
    } catch (SQLException e) {
      UltraDaycare.LOGGER.error("Error initializing SQL database table: ", e);
    }
  }

  @Override
  public CompletableFuture<@Nullable User> find(UUID uuid) {
    User cached = DatabaseClient.USERS.getIfPresent(uuid);
    if (cached != null) return CompletableFuture.completedFuture(cached);

    return CompletableFuture.supplyAsync(() -> {
      String sql = "SELECT data FROM ultradaycare_users WHERE player_uuid = ?";
      try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, uuid.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            String json = rs.getString("data");
            if (json != null && !json.isEmpty()) {
              return UtilsFile.getGson().fromJson(json, User.class);
            }
          }
        }
      } catch (SQLException e) {
        UltraDaycare.LOGGER.error("Error finding user in SQL database: ", e);
      }
      return null;
    });
  }

  private String getUpsertQuery() {
    if (config == null || config.getType() == null) {
      return "INSERT INTO ultradaycare_users (player_uuid, player_name, data, updated_at) VALUES (?, ?, ?, ?) " +
        "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, data = excluded.data, updated_at = excluded.updated_at";
    }

    return switch (config.getType()) {
      case H2 ->
        "MERGE INTO ultradaycare_users (player_uuid, player_name, data, updated_at) KEY(player_uuid) VALUES (?, ?, ?, ?)";
      case SQLITE ->
        "INSERT INTO ultradaycare_users (player_uuid, player_name, data, updated_at) VALUES (?, ?, ?, ?) " +
          "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, data = excluded.data, updated_at = excluded.updated_at";
      case MYSQL, MARIADB ->
        "INSERT INTO ultradaycare_users (player_uuid, player_name, data, updated_at) VALUES (?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), data = VALUES(data), updated_at = VALUES(updated_at)";
      default ->
        "INSERT INTO ultradaycare_users (player_uuid, player_name, data, updated_at) VALUES (?, ?, ?, ?) " +
          "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, data = excluded.data, updated_at = excluded.updated_at";
    };
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(User user) {
    if (user == null || user.getPlayerUUID() == null) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(() -> {
      String json = UtilsFile.getGson().toJson(user);
      String sql = getUpsertQuery();

      try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, user.getPlayerUUID().toString());
        pstmt.setString(2, user.getPlayerName() != null ? user.getPlayerName() : "");
        pstmt.setString(3, json);
        pstmt.setLong(4, System.currentTimeMillis());
        pstmt.executeUpdate();
      } catch (SQLException e) {
        UltraDaycare.LOGGER.error("Error saving user to SQL database: ", e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveAll() {
    List<CompletableFuture<Void>> futures = DatabaseClient.USERS.asMap().values().stream()
      .map(this::saveOrUpdateUser)
      .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  @Override
  public CompletableFuture<Boolean> delete(UUID uuid) {
    DatabaseClient.USERS.invalidate(uuid);
    return CompletableFuture.supplyAsync(() -> {
      String sql = "DELETE FROM ultradaycare_users WHERE player_uuid = ?";
      try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, uuid.toString());
        return pstmt.executeUpdate() > 0;
      } catch (SQLException e) {
        UltraDaycare.LOGGER.error("Error deleting user from SQL database: ", e);
        return false;
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> exists(UUID uuid) {
    if (DatabaseClient.USERS.getIfPresent(uuid) != null) {
      return CompletableFuture.completedFuture(true);
    }
    return CompletableFuture.supplyAsync(() -> {
      String sql = "SELECT 1 FROM ultradaycare_users WHERE player_uuid = ?";
      try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, uuid.toString());
        try (ResultSet rs = pstmt.executeQuery()) {
          return rs.next();
        }
      } catch (SQLException e) {
        return false;
      }
    });
  }
}
