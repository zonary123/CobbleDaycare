package com.kingpixel.ultradaycare.database.repository.impl;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBManager;
import com.kingpixel.cobbleutils.util.mongodb.MongoDBService;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.UserRepository;
import com.kingpixel.ultradaycare.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MongoUserRepository implements UserRepository using CobbleUtils MongoDBService.
 *
 * @author Carlos Varas Alonso
 */
public class MongoUserRepository implements UserRepository {
  private final MongoDBManager manager;
  private final MongoCollection<Document> collection;

  public MongoUserRepository(DataBaseConfig config) {
    this.manager = MongoDBService.getOrCreateManager(config);
    this.collection = manager.getCollection(config.getDatabase(), "user_information");
  }

  @Override
  public CompletableFuture<@Nullable User> find(UUID uuid) {
    return manager.supplyAsync(() -> {
      User user = DatabaseClient.USERS.getIfPresent(uuid);
      if (user != null) return user;
      Document document = collection.find(Filters.eq("playerUUID", uuid.toString())).first();
      if (document == null) return null;
      return User.fromDocument(document);
    });
  }

  @Override
  public CompletableFuture<Void> saveOrUpdateUser(User user) {
    return manager.runAsync(() -> {
      Bson filter = Filters.eq("playerUUID", user.getPlayerUUID().toString());
      Document document = user.toDocument();
      collection.replaceOne(filter, document, new ReplaceOptions().upsert(true));
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
    return manager.supplyAsync(() -> {
      Bson filter = Filters.eq("playerUUID", uuid.toString());
      return collection.deleteOne(filter).getDeletedCount() > 0;
    });
  }

  @Override
  public CompletableFuture<Boolean> exists(UUID uuid) {
    if (DatabaseClient.USERS.getIfPresent(uuid) != null) {
      return CompletableFuture.completedFuture(true);
    }
    return manager.supplyAsync(() -> {
      Bson filter = Filters.eq("playerUUID", uuid.toString());
      return collection.countDocuments(filter) > 0;
    });
  }
}
