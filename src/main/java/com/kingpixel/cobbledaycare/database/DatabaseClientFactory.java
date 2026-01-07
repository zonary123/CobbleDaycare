package com.kingpixel.cobbledaycare.database;


import com.kingpixel.cobbleutils.Model.DataBaseConfig;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:03
 */
public class DatabaseClientFactory {


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

