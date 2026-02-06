package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
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
  private long connectedTime;
  private boolean notifyCreateEgg;
  private boolean notifyLimitEggs;
  private boolean notifyBanPokemon;
  private boolean actionBar;
  private float multiplierSteps;
  private long timeMultiplierSteps;
  private long cooldownHatch;
  private long cooldownBreed;
  private ArrayList<Plot> plots;

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

  public synchronized static UserInformation fromDocument(Document document) {
    return GSON.fromJson(document.toJson(), UserInformation.class);
  }

  public synchronized Document toDocument() {
    return Document.parse(GSON.toJson(this));
  }

  public float getActualMultiplier(ServerPlayerEntity player) {
    float multiplier = CobbleDaycare.config.getMultiplierSteps();
    for (Map.Entry<String, Float> entry : CobbleDaycare.config.getMultiplierStepsPermission().entrySet()) {
      if (entry.getValue() <= multiplier) continue;
      if (PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        multiplier = entry.getValue();
      }
    }
    return Math.max(multiplier, this.multiplierSteps);
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
    if (player == null) throw new IllegalArgumentException("The player cannot be null.");

    boolean update = false;


    if (plots == null) {
      CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID, "UserInformation plots was null for player " + player.getGameProfile().getName() + " fixing...");
      plots = new ArrayList<>();
      update = true;
    }

    if (playerUUID == null || playerName == null) {
      playerUUID = player.getUuid();
      playerName = player.getGameProfile().getName();
      update = true;
    }

    int currentSize = plots.size();

    if (currentSize < numPlots) {
      for (int i = currentSize; i < numPlots; i++) {
        plots.add(new Plot());
      }
      update = true;
    } else if (currentSize > numPlots) {
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      for (int i = currentSize - 1; i >= numPlots; i--) {
        Plot plot = plots.remove(i);
        if (plot == null) continue;

        returnToParty(player, party, plot.getMale(), "male");
        returnToParty(player, party, plot.getFemale(), "female");
        plot.giveEggs(player);
      }
      update = true;
    }

    return update;
  }

  private void returnToParty(ServerPlayerEntity player, PlayerPartyStore party, Pokemon pokemon, String type) {
    if (pokemon == null) return;

    String name = type.equals("egg") ?
      pokemon.getPersistentData().getString(DayCarePokemon.TAG_POKEMON) :
      pokemon.getDisplayName(false).getString();

    PlayerUtils.sendMessage(
      player,
      "Your " + type + " " + name + " has been returned to your party because the plot has been removed.",
      CobbleDaycare.language.getPrefix(),
      TypeMessage.CHAT
    );
    CobbleUtils.server.execute(() -> party.add(pokemon));
  }


  public synchronized boolean fix(ServerPlayerEntity player) {
    boolean update = false;
    if (plots == null) {
      CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID, "UserInformation plots was null for player " + player.getGameProfile().getName() + " fixing...");
      plots = new ArrayList<>();
      update = true;
    }
    for (Plot plot : plots) {
      var eggsIterator = plot.getEggs().iterator();
      while (eggsIterator.hasNext()) {
        Pokemon egg = eggsIterator.next();
        if (egg == null) {
          eggsIterator.remove();
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
