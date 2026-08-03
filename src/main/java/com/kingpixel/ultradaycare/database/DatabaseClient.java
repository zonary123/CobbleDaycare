package com.kingpixel.ultradaycare.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultradaycare.database.repository.UserRepository;
import com.kingpixel.ultradaycare.models.User;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * DatabaseClient acts as the central database manager and delegates user persistence to UserRepository.
 *
 * @author Carlos Varas Alonso
 */
@Getter
public abstract class DatabaseClient {
  public static final Cache<UUID, User> USERS = Caffeine.newBuilder()
    .maximumSize(200)
    .build();

  protected UserRepository userRepository;

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  public @Nullable User getUser(ServerPlayerEntity player) {
    return getUser(player.getUuid());
  }

  public @Nullable User getUser(UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  public CompletableFuture<@Nullable User> find(ServerPlayerEntity player) {
    return find(player.getUuid());
  }

  public CompletableFuture<@Nullable User> find(UUID uuid) {
    if (userRepository != null) {
      return userRepository.find(uuid);
    }
    return CompletableFuture.completedFuture(USERS.getIfPresent(uuid));
  }

  public CompletableFuture<Void> saveOrUpdateUser(User user) {
    if (userRepository != null) {
      return userRepository.saveOrUpdateUser(user);
    }
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<Void> saveAll() {
    if (userRepository != null) {
      return userRepository.saveAll();
    }
    List<CompletableFuture<Void>> futures = USERS.asMap().values().stream()
      .map(user -> CompletableFuture.runAsync(() -> saveOrUpdateUser(user)))
      .toList();

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }
}
