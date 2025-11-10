package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Improved by GitHub Copilot - 07/08/2024 9:41
 */
public class JSONClient extends DatabaseClient {

  public JSONClient(DataBaseConfig config) {
  }


  @Override public void connect(DataBaseConfig config) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Connected to JSON database at path: " + CobbleDaycare.PATH_DATA);
  }

  @Override public void disconnect() {
    var entries = DatabaseClientFactory.USER_INFORMATION_MAP.asMap().entrySet();
    for (var entry : entries) {
      var key = entry.getKey();
      var value = entry.getValue();
      saveOrUpdateUserInformation(
        key,
        value
      );
    }
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Disconnected from JSON database.");
  }

  @Override public UserInformation getUserInformation(ServerPlayerEntity player) {
    UserInformation userInformation = DatabaseClientFactory.USER_INFORMATION_MAP.getIfPresent(player.getUuid());
    if (userInformation == null) {
      readFile(player);
      userInformation = DatabaseClientFactory.USER_INFORMATION_MAP.getIfPresent(player.getUuid());
    }
    return userInformation;
  }

  private void readFile(ServerPlayerEntity player) {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", call -> {
        UserInformation userInformation = UserInformation.GSON.fromJson(call, UserInformation.class);
        if (userInformation == null) {
          userInformation = new UserInformation(player);
          CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
            CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", UserInformation.GSON.toJson(userInformation)
          );
          futureWrite.join();
        }
        DatabaseClientFactory.USER_INFORMATION_MAP.put(player.getUuid(), userInformation);
      }
    );

    if (!futureRead.join()) {
      UserInformation userInformation = new UserInformation(player);
      DatabaseClientFactory.USER_INFORMATION_MAP.put(player.getUuid(), userInformation);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", Utils.newWithoutSpacingGson().toJson(userInformation)
      );
      futureWrite.join();
    }
  }


  @Override public void saveOrUpdateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    if (player == null || userInformation == null) return;
    saveOrUpdateUserInformation(player.getUuid(), userInformation);
  }

  private void saveOrUpdateUserInformation(UUID playerUUID, UserInformation userInformation) {
    DatabaseClientFactory.USER_INFORMATION_MAP.put(playerUUID, userInformation);
    Utils.writeFileAsync(
      Utils.getAbsolutePath(CobbleDaycare.PATH_DATA + playerUUID.toString() + ".json"),
      UserInformation.GSON.toJson(userInformation)
    );
  }
}