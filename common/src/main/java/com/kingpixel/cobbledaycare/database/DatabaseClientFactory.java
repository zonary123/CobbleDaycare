package com.kingpixel.cobbledaycare.database;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {
  public static final Cache<UUID, UserInformation> USER_INFORMATION_MAP = Caffeine.newBuilder()
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .removalListener((key, value, cause) -> {
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "UserInformation for player " + key + " removed from cache due to " + cause);
      }
    })
    .build();
  public static DatabaseClient INSTANCE;

  public synchronized static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (INSTANCE != null) INSTANCE.disconnect();
    INSTANCE = null;
    switch (database.getType()) {
      case MONGODB -> INSTANCE = new MongoDBClient();
      case JSON -> INSTANCE = new JSONClient();
      default -> {
        throw new IllegalArgumentException("Unsupported database type: " + database.getType());
      }
    }
    INSTANCE.connect(database);
    return INSTANCE;
  }

}

