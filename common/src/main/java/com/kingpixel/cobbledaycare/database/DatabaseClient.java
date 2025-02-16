package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public interface DatabaseClient {
  void connect(DataBaseConfig config);

  void disconnect();

  void save();

  UserInformation getUserInformation(ServerPlayerEntity player);

  UserInformation updateUserInformation(ServerPlayerEntity player, UserInformation userInformation);

  void removeIfNecessary(ServerPlayerEntity player);
}
