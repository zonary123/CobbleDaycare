package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
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
  private float multiplierSteps;
  private long timeMultiplierSteps;
  private List<Plot> plots;

  public UserInformation() {
    this.playerUUID = null;
    this.playerName = null;
    this.notifyLimitEggs = true;
    this.notifyCreateEgg = true;
    this.notifyBanPokemon = true;
    this.multiplierSteps = 1.0f;
    this.timeMultiplierSteps = 0;
    this.plots = new ArrayList<>();
  }

  public UserInformation(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    this.playerName = player.getGameProfile().getName();
    this.notifyLimitEggs = true;
    this.notifyCreateEgg = true;
    this.notifyBanPokemon = true;
    this.multiplierSteps = 1.0f;
    this.timeMultiplierSteps = 0;
    this.plots = new ArrayList<>();
  }

  public double getActualMultiplier() {
    return Math.max(CobbleDaycare.config.getMultiplierSteps(), multiplierSteps);
  }

  public boolean check(int numPlots, ServerPlayerEntity player) {
    boolean update = false;
    if (playerUUID == null || playerName == null) {
      playerUUID = player.getUuid();
      playerName = player.getGameProfile().getName();
      update = true;
    }
    // Add plots if the user has fewer plots than numPlots
    if (plots.size() < numPlots) {
      for (int i = plots.size(); i < numPlots; i++) {
        plots.add(new Plot());
        update = true;
      }
    }

    // Transfer Pokémon and eggs if the user has more plots than numPlots
    if (plots.size() > numPlots) {
      for (int i = plots.size() - 1; i >= numPlots; i--) {
        Plot plot = plots.get(i);
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        // Transfer Pokémon and eggs to the player
        party.add(plot.getMale());
        party.add(plot.getFemale());
        for (Pokemon egg : plot.getEggs()) party.add(egg);
        // Remove the excess plot
        plots.remove(i);
        update = true;
      }
    }

    return update;
  }
}
