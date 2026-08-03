package com.kingpixel.ultradaycare.database.repository.impl;

import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.UserRepository;
import com.kingpixel.ultradaycare.models.User;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JsonUserRepository implements UserRepository for JSON file storage.
 *
 * @author Carlos Varas Alonso
 */
public class JsonUserRepository implements UserRepository {
  private static final Path PATH = UltraDaycare.getPath().resolve("data");

  private Path getPath(UUID uuid) {
    return PATH.resolve(uuid + ".json");
  }

  @Override
  public CompletableFuture<@Nullable User> find(UUID uuid) {
    User user = DatabaseClient.USERS.getIfPresent(uuid);
    if (user != null) return CompletableFuture.completedFuture(user);
    return UtilsFile.readAsync(getPath(uuid), User.class);
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(User user) {
    return UtilsFile.writeAsync(getPath(user.getPlayerUUID()), user);
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
      try {
        return Files.deleteIfExists(getPath(uuid));
      } catch (Exception e) {
        return false;
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> exists(UUID uuid) {
    if (DatabaseClient.USERS.getIfPresent(uuid) != null) {
      return CompletableFuture.completedFuture(true);
    }
    return CompletableFuture.supplyAsync(() -> Files.exists(getPath(uuid)));
  }
}
