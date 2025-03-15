package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
  private long cooldownHatch;
  private long cooldownBreed;
  private List<Plot> plots;

  public UserInformation() {
    this.playerUUID = null;
    this.playerName = null;
    this.notifyLimitEggs = true;
    this.notifyCreateEgg = true;
    this.notifyBanPokemon = true;
    this.multiplierSteps = 1.0f;
    this.timeMultiplierSteps = 0;
    this.cooldownHatch = 0;
    this.cooldownBreed = 0;
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
    this.cooldownHatch = 0;
    this.cooldownBreed = 0;
    this.plots = new ArrayList<>();
  }

  public double getActualMultiplier() {
    return Math.max(CobbleDaycare.config.getMultiplierSteps(), multiplierSteps);
  }

  public boolean hasCooldownHatch(ServerPlayerEntity player) {
    if (PermissionApi.hasPermission(player, "cobbledaycare.hatch.bypass", 4)) return false;
    return cooldownHatch > System.currentTimeMillis();
  }

  public void setCooldownHatch(ServerPlayerEntity player) {
    this.cooldownHatch = System.currentTimeMillis() +
      TimeUnit.SECONDS.toMillis(PlayerUtils.getCooldown(CobbleDaycare.config.getCooldownsHatch(),
        CobbleDaycare.config.getDefaultCooldownHatch(), player));
  }

  public boolean hasCooldownBreed(ServerPlayerEntity player) {
    if (PermissionApi.hasPermission(player, "cobbledaycare.breed.bypass", 4)) return false;
    return cooldownBreed > System.currentTimeMillis();
  }

  public void setCooldownBreed(ServerPlayerEntity player) {
    this.cooldownBreed =
      System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PlayerUtils.getCooldown(CobbleDaycare.config.getCooldownsBreed(),
        CobbleDaycare.config.getDefaultCooldownBreed(), player));
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
