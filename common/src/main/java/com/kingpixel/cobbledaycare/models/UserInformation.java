package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 31/01/2025 1:16
 */
@Getter
@Setter
@Data
@ToString
public class UserInformation {
  public static final Gson GSON = Utils.newWithoutSpacingGson().newBuilder()
    .serializeNulls().create();
  private UUID playerUUID;
  private String playerName;
  private String country;
  private boolean notifyCreateEgg;
  private boolean notifyLimitEggs;
  private boolean notifyBanPokemon;
  private boolean actionBar;
  private float multiplierSteps;
  private long timeMultiplierSteps;
  private long cooldownHatch;
  private long cooldownBreed;
  private List<Plot> plots;

  public UserInformation() {
    this.playerUUID = null;
    this.playerName = null;
    this.multiplierSteps = 1.0f;
    this.timeMultiplierSteps = 0;
    this.cooldownHatch = 0;
    this.cooldownBreed = 0;
    var userInfoOptions = CobbleDaycare.config.getUserInfoOptions();
    this.notifyBanPokemon = userInfoOptions.isNotifyBanPokemon();
    this.notifyCreateEgg = userInfoOptions.isNotifyCreateEgg();
    this.actionBar = userInfoOptions.isActionBar();
    this.plots = new ArrayList<>();
  }

  public UserInformation(ServerPlayerEntity player) {
    super();
    this.playerUUID = player.getUuid();
    this.playerName = player.getGameProfile().getName();
  }

  public static UserInformation fromDocument(Document document) {
    return GSON.fromJson(document.toJson(), UserInformation.class);
  }

  public Document toDocument() {
    return Document.parse(GSON.toJson(this));
  }

  public float getActualMultiplier(ServerPlayerEntity player) {
    float steps = CobbleDaycare.config.getMultiplierSteps();
    for (Map.Entry<String, Float> entry : CobbleDaycare.config.getMultiplierStepsPermission().entrySet()) {
      if (steps <= entry.getValue() && PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        steps = entry.getValue();
      }
    }
    return Math.max(steps, multiplierSteps);
  }

  public boolean hasCooldownHatch(ServerPlayerEntity player) {
    if (PermissionApi.hasPermission(player, "cobbledaycare.hatch.bypass", 4)) return false;
    return cooldownHatch > System.currentTimeMillis();
  }

  public void setCooldownHatch(ServerPlayerEntity player) {
    this.cooldownHatch = System.currentTimeMillis() +
      PlayerUtils.getCooldown(CobbleDaycare.config.getCooldownsHatch(),
        CobbleDaycare.config.getDefaultCooldownHatch(), player);
  }

  public boolean hasCooldownBreed(ServerPlayerEntity player) {
    if (PermissionApi.hasPermission(player, "cobbledaycare.breed.bypass", 4)) return false;
    return cooldownBreed > System.currentTimeMillis();
  }

  public void setCooldownBreed(ServerPlayerEntity player) {
    this.cooldownBreed =
      System.currentTimeMillis() + PlayerUtils.getCooldown(CobbleDaycare.config.getCooldownsBreed(),
        CobbleDaycare.config.getDefaultCooldownBreed(), player);
  }

  public synchronized boolean check(int numPlots, ServerPlayerEntity player) {
    boolean update = false;

    // Asegurar que la lista esté inicializada
    if (plots == null) {
      plots = new ArrayList<>();
      update = true;
    }

    // Asegurar que el jugador esté inicializado
    if (player == null) {
      throw new IllegalArgumentException("El jugador no puede ser nulo.");
    }

    // Validar y actualizar datos del jugador
    if (playerUUID == null || playerName == null) {
      playerUUID = player.getUuid();
      playerName = player.getGameProfile().getName();
      update = true;
    }

    // Obtener el número actual de plots
    int currentSize = plots.size();

    // Si faltan plots, añadirlos
    if (currentSize < numPlots) {
      for (int i = currentSize; i < numPlots; i++) {
        plots.add(new Plot());
      }
      update = true;
    }

    // Si sobran plots, transferir Pokémon y eliminarlos
    else if (currentSize > numPlots) {
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      for (int i = currentSize - 1; i >= numPlots; i--) {
        Plot plot = plots.get(i);

        if (plot != null) {
          if (plot.getMale() != null) {
            PlayerUtils.sendMessage(
              player,
              "Your Pokémon " + plot.getMale().getDisplayName().getString() + " has been returned to your party because the plot has been removed.",
              CobbleDaycare.language.getPrefix(),
              TypeMessage.CHAT
            );
            party.add(plot.getMale());
          }
          if (plot.getFemale() != null) {
            PlayerUtils.sendMessage(
              player,
              "Your Pokémon " + plot.getFemale().getDisplayName().getString() + " has been returned to your party because the plot has been removed.",
              CobbleDaycare.language.getPrefix(),
              TypeMessage.CHAT
            );
            party.add(plot.getFemale());
          }
          for (Pokemon egg : plot.getEggs()) {
            if (egg != null) {
              PlayerUtils.sendMessage(
                player,
                "Your egg " + egg.getPersistentData().getString(DayCarePokemon.TAG_POKEMON) + " has been returned to your party because the plot has been removed.",
                CobbleDaycare.language.getPrefix(),
                TypeMessage.CHAT
              );
              party.add(egg);
            }
          }
        }

        plots.remove(i);
      }
      update = true;
    }

    return update;
  }


  public synchronized boolean fix(ServerPlayerEntity player) {
    boolean update = false;
    for (Plot plot : plots) {
      for (Pokemon egg : plot.getEggs()) {
        if (egg == null) {
          plot.getEggs().remove(egg);
          update = true;
        }
      }
      if (plot.checkEgg(player, this)) update = true;
    }
    return update;
  }

  public CharSequence getIndexPlot(Plot plot) {
    int index = plots.indexOf(plot) + 1;
    return index + "";
  }
}
