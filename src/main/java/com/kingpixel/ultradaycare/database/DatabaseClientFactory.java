package com.kingpixel.ultradaycare.database;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.service.JSONService;
import com.kingpixel.ultradaycare.database.service.MongoDBService;
import com.kingpixel.ultradaycare.database.service.SQLService;

/**
 * DatabaseClientFactory instantiates database services according to DataBaseConfig type.
 *
 * @author Carlos Varas Alonso
 */
public class DatabaseClientFactory {

  public synchronized static DatabaseClient createDatabaseClient(DataBaseConfig database) {
    if (UltraDaycare.database != null) UltraDaycare.database.disconnect();
    UltraDaycare.database = null;

    if (database == null || database.getType() == null) {
      UltraDaycare.database = new JSONService();
      UltraDaycare.database.connect(database);
      return UltraDaycare.database;
    }

    switch (database.getType()) {
      case MONGODB -> UltraDaycare.database = new MongoDBService();
      case JSON -> UltraDaycare.database = new JSONService();
      case SQLITE, H2, MYSQL, MARIADB -> UltraDaycare.database = new SQLService();
      default -> UltraDaycare.database = new JSONService();
    }

    UltraDaycare.database.connect(database);
    return UltraDaycare.database;
  }
}
