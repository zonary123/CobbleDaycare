package com.kingpixel.cobbledaycare.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 24/07/2024 21:02
 */
public abstract class DatabaseClient {
  public static final Cache<UUID, UserInformation> USERS = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.SECONDS)
    .removalListener((key, value, cause) -> {
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "UserInformation for player " + key + " removed from cache due to " + cause);
      }
    })
    .build();

  public abstract void connect(DataBaseConfig config);

  public abstract void disconnect();

  public abstract UserInformation getUserInformation(ServerPlayerEntity player);

  public abstract void saveOrUpdateUserInformation(ServerPlayerEntity player, UserInformation userInformation);

  public void removeFromCache(ServerPlayerEntity player) {
    USERS.invalidate(player.getUuid());
  }
}
