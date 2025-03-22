package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Improved by GitHub Copilot - 07/08/2024 9:41
 */
public class JSONClient implements DatabaseClient {

  public JSONClient(DataBaseConfig config) {
  }


  @Override public void connect(DataBaseConfig config) {

  }

  @Override public void disconnect() {

  }

  @Override public void save() {

  }

  @Override public UserInformation getUserInformation(ServerPlayerEntity player) {
    UserInformation userInformation = DatabaseClientFactory.userPlots.get(player.getUuid());
    if (userInformation == null) {
      readFile(player);
      userInformation = DatabaseClientFactory.userPlots.get(player.getUuid());
    }
    return userInformation;
  }

  private void readFile(ServerPlayerEntity player) {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", call -> {
        UserInformation userInformation = Utils.newWithoutSpacingGson().fromJson(call, UserInformation.class);
        if (userInformation == null) {
          userInformation = new UserInformation(player);
          CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
            CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", Utils.newWithoutSpacingGson().toJson(userInformation)
          );
          futureWrite.join();
        }
        DatabaseClientFactory.userPlots.put(player.getUuid(), userInformation);
      }
    );

    if (!futureRead.join()) {
      UserInformation userInformation = new UserInformation(player);
      DatabaseClientFactory.userPlots.put(player.getUuid(), userInformation);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH_DATA, player.getUuidAsString() + ".json", Utils.newWithoutSpacingGson().toJson(userInformation)
      );
      futureWrite.join();
    }
  }


  @Override public UserInformation updateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    if (player == null || userInformation == null) return null;
    DatabaseClientFactory.userPlots.put(player.getUuid(), userInformation);
    CompletableFuture.runAsync(() -> {
      Utils.writeFileSync(
        Utils.getAbsolutePath(CobbleDaycare.PATH_DATA + player.getUuid().toString() + ".json"),
        Utils.newWithoutSpacingGson().toJson(userInformation)
      );
    });
    return userInformation;
  }

  @Override public void removeIfNecessary(ServerPlayerEntity player) {
    DatabaseClientFactory.userPlots.remove(player.getUuid());
  }
}