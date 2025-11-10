package com.kingpixel.cobbledaycare.database;


import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {
  public static final Map<UUID, UserInformation> USER_INFORMATION_MAP = new ConcurrentHashMap<>();
  public static DatabaseClient INSTANCE;

  public synchronized static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (INSTANCE != null) INSTANCE.disconnect();
    INSTANCE = null;
    switch (database.getType()) {
      case MONGODB -> INSTANCE = new MongoDBClient(database);
      case JSON -> INSTANCE = new JSONClient(database);
      default -> {
        throw new IllegalArgumentException("Unsupported database type: " + database.getType());
      }
    }
    INSTANCE.connect(database);
    return INSTANCE;
  }

}

