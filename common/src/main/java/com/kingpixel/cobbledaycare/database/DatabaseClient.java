package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public abstract class DatabaseClient {
  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  public abstract UserInformation getUserInformation(ServerPlayerEntity player);

  public abstract void saveOrUpdateUserInformation(ServerPlayerEntity player, UserInformation userInformation);

  public UserInformation removeFromCache(ServerPlayerEntity player) {
    return DatabaseClientFactory.USER_INFORMATION_MAP.remove(player.getUuid());
  }
}
