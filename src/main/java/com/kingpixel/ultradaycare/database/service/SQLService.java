package com.kingpixel.ultradaycare.database.service;

import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.database.DatabaseClient;
import com.kingpixel.ultradaycare.database.repository.impl.SqlUserRepository;

/**
 * SQLService manages SQL database connections for H2, SQLite, MySQL, MariaDB, and PostgreSQL.
 *
 * @author Carlos Varas Alonso
 */
public class SQLService extends DatabaseClient {

  public SQLService() {
  }

  @Override
  public void connect(DataBaseConfig config) {
    this.userRepository = new SqlUserRepository(config);
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Connected to SQL database repository (" + (config.getType() != null ? config.getType() : "SQL") + ").");
  }

  @Override
  public void disconnect() {
    saveAll().join();
    UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "Disconnected from SQL database.");
  }
}
