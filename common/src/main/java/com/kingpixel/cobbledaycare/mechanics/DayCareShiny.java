package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareShiny extends Mechanics {
  public static final String TAG = "shiny";
  private boolean masuda;
  private boolean parentsShiny;
  private float percentageShiny;
  private Map<String, Float> percentagesShiny;
  private float multiplierShiny;
  private float multiplierMasuda;

  public DayCareShiny() {
    this.masuda = true;
    this.parentsShiny = true;
    this.percentageShiny = 8192;
    this.percentagesShiny = Map.of(
      "cobbledaycare.shinyboost.vip", percentageShiny
    );
    this.multiplierShiny = 1.5f;
    this.multiplierMasuda = 1.5f;
  }

  private float getShinyRate(ServerPlayerEntity player) {
    if (player == null) return percentageShiny;
    float shinyRate = percentageShiny;
    for (Map.Entry<String, Float> entry : percentagesShiny.entrySet()) {
      if (PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        if (shinyRate > entry.getValue()) shinyRate = entry.getValue();
      }
    }
    return shinyRate;
  }

  @Override public String replace(String text, ServerPlayerEntity player) {
    return text
      .replace("%shinyrate%", String.format("%.2f", getShinyRate(player)))
      .replace("%multiplierShiny%", String.format("%.2f", multiplierShiny))
      .replace("%multiplierMasuda%", String.format("%.2f", multiplierMasuda))
      .replace("%multipliershiny%", String.format("%.2f", multiplierShiny))
      .replace("%multipliermasuda%", String.format("%.2f", multiplierMasuda))
      .replace("%masuda%", masuda ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())
      .replace("%parentsShiny%", parentsShiny ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon male = builder.getMale();
    Pokemon female = builder.getFemale();
    Pokemon egg = builder.getEgg();
    float shinyrate = getShinyRate(builder.getPlayer());
    float multiplier = getMultiplierShiny();

    if (multiplier > 0 && isParentsShiny()) {
      if (male.getShiny()) shinyrate /= multiplier;
      if (female.getShiny()) shinyrate /= multiplier;
    }

    if (isMasuda()) {
      String maleCountry = male.getPersistentData().getString(DayCareCountry.TAG);
      String femaleCountry = female.getPersistentData().getString(DayCareCountry.TAG);
      if (!maleCountry.isEmpty() && !femaleCountry.isEmpty()) {
        if (!maleCountry.equalsIgnoreCase(femaleCountry)) shinyrate /= getMultiplierMasuda();
      }
    }

    shinyrate = (int) Math.max(1, shinyrate);
    if (shinyrate <= 1) {
      egg.setShiny(true);
    } else {
      egg.setShiny(Utils.RANDOM.nextInt((int) shinyrate) == 0);
    }
    egg.getPersistentData().putBoolean(TAG, egg.getShiny());
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    boolean shiny = egg.getPersistentData().getBoolean(TAG);
    egg.setShiny(shiny);
    egg.getPersistentData().remove(TAG);
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    egg.setShiny(pokemon.getShiny());
    egg.getPersistentData().putBoolean(TAG, pokemon.getShiny());
  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%shiny%", nbt.getBoolean(TAG) ? CobbleUtils.language.getSymbolshiny() : "");
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "shiny";
  }
}
