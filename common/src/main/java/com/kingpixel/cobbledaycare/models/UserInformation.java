package com.kingpixel.cobbledaycare.models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 31/01/2025 1:16
 */
@Getter
@Setter
@Data
@ToString
public class UserInformation {
  private UUID playerUUID;
  private String playerName;
  private boolean notifyCreateEgg;
  private boolean notifyLimitEggs;
  private boolean notifyBanPokemon;
  private List<Plot> plots;

  public UserInformation(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    this.playerName = player.getGameProfile().getName();
    this.notifyLimitEggs = true;
    this.notifyCreateEgg = true;
    this.notifyBanPokemon = true;
    this.plots = new ArrayList<>();
  }

  public boolean check(int size) {
    boolean update = false;
    if (plots.size() < size) {
      for (int i = plots.size(); i < size; i++) {
        plots.add(new Plot());
        update = true;
      }
    }
    return update;
  }
}
