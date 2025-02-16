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
  public static DatabaseClient databaseClient;
  public static Map<UUID, UserInformation> userPlots = new HashMap<>();

  public static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (databaseClient != null) {
      databaseClient.disconnect();
    }
    switch (database.getType()) {
      case MONGODB -> databaseClient = new MongoDBClient(database);
      case JSON -> databaseClient = new JSONClient(database);
      default -> databaseClient = new JSONClient(database);
    }
    databaseClient.connect(database);
    return databaseClient;
  }

}

