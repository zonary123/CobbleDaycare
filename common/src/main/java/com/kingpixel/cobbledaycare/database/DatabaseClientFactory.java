package com.kingpixel.cobbledaycare.database;


import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {
  public static DatabaseClient INSTANCE;
  public static Map<UUID, UserInformation> userPlots = new HashMap<>();

  public static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (INSTANCE != null) {
      INSTANCE.disconnect();
    }
    switch (database.getType()) {
      case MONGODB -> INSTANCE = new MongoDBClient(database);
      case JSON -> INSTANCE = new JSONClient(database);
      default -> INSTANCE = new JSONClient(database);
    }
    INSTANCE.connect(database);
    return INSTANCE;
  }

}

