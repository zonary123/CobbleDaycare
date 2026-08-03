package com.kingpixel.ultradaycare.database.repository;

import com.kingpixel.ultradaycare.models.User;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * UserRepository interface for UltraDayCare user persistence.
 *
 * @author Carlos Varas Alonso
 */
public interface UserRepository {

  CompletableFuture<@Nullable User> find(UUID uuid);

  CompletableFuture<Void> saveOrUpdateUser(User user);

  CompletableFuture<Void> saveAll();

  CompletableFuture<Boolean> delete(UUID uuid);

  CompletableFuture<Boolean> exists(UUID uuid);
}
