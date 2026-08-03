package com.kingpixel.ultradaycare.database.service;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.impl.MongoUserRepository;

/**
 * MongoDBService manages MongoDB database connections.
 *
 * @author Carlos Varas Alonso
 */
public class MongoDBService extends DatabaseClient {

  public MongoDBService() {
  }

  @Override
  public void connect(DataBaseConfig config) {
    this.userRepository = new MongoUserRepository(config);
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Connected to MongoDB database repository.");
  }

  @Override
  public void disconnect() {
    saveAll().join();
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Disconnected from MongoDB database.");
  }
}
