package com.kingpixel.ultradaycare.database.service;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.impl.JsonUserRepository;

/**
 * JSONService manages JSON database connections.
 *
 * @author Carlos Varas Alonso
 */
public class JSONService extends DatabaseClient {

  public JSONService() {
    this.userRepository = new JsonUserRepository();
  }

  @Override
  public void connect(DataBaseConfig config) {
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Connected to JSON database repository.");
  }

  @Override
  public void disconnect() {
    saveAll().join();
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Disconnected from JSON database.");
  }
}
