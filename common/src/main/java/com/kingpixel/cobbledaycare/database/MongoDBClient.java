package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import net.minecraft.server.network.ServerPlayerEntity;

public class MongoDBClient implements DatabaseClient {

  public MongoDBClient(DataBaseConfig config) {
  }

  @Override public void connect(DataBaseConfig config) {

  }

  @Override public void disconnect() {

  }

  @Override public void save() {

  }

  @Override public UserInformation getUserInformation(ServerPlayerEntity player) {
    return null;
  }

  @Override public UserInformation updateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    return null;
  }

  @Override public void removeIfNecessary(ServerPlayerEntity player) {
    DatabaseClientFactory.userPlots.remove(player.getUuid());
  }

}
