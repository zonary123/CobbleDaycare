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

  public JSONClient() {
  }


  @Override public void connect(DataBaseConfig config) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Connected to JSON database at path: " + CobbleDaycare.PATH_DATA);
  }

  @Override public void disconnect() {
    var entries = DatabaseClient.USERS.asMap().entrySet();
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
    UserInformation userInformation = DatabaseClient.USERS.getIfPresent(player.getUuid());
    if (userInformation == null) {
      readFile(player);
      userInformation = DatabaseClient.USERS.getIfPresent(player.getUuid());
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
        DatabaseClient.USERS.put(player.getUuid(), userInformation);
      }
    );

    if (Boolean.FALSE.equals(futureRead.join())) {
      UserInformation userInformation = new UserInformation(player);
      DatabaseClient.USERS.put(player.getUuid(), userInformation);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", Utils.newWithoutSpacingGson().toJson(userInformation)
      );
      futureWrite.join();
    }
  }


  @Override public void saveOrUpdateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    if (player == null || userInformation == null) return;
    CobbleDaycare.runAsync(() -> saveOrUpdateUserInformation(player.getUuid(), userInformation));
  }

  private void saveOrUpdateUserInformation(UUID playerUUID, UserInformation userInformation) {
    DatabaseClient.USERS.put(playerUUID, userInformation);
    Utils.writeFileSync(
      Utils.getAbsolutePath(CobbleDaycare.PATH_DATA + playerUUID.toString() + ".json"),
      UserInformation.GSON.toJson(userInformation)
    );
  }

}