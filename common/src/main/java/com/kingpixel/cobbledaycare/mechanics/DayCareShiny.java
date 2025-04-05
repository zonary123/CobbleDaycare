package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareShiny extends Mechanics {
  public static final String TAG = "shiny";
  private boolean masuda;
  private boolean parentsShiny;
  private float percentageShiny;
  private float multiplierShiny;
  private float multiplierMasuda;

  public DayCareShiny() {
    this.masuda = true;
    this.parentsShiny = true;
    this.percentageShiny = 8192;
    this.multiplierShiny = 1.5f;
    this.multiplierMasuda = 1.5f;
  }

  @Override public String replace(String text) {
    return text
      .replace("%shinyrate%", String.format("%.2f", percentageShiny))
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
    float shinyrate = getPercentageShiny();
    float multiplier = getMultiplierShiny();

    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "DayCareShiny -> applyEgg -> shinyrate: " + shinyrate);
    }

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
    egg.getPersistentData().putBoolean(TAG, pokemon.getShiny());
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "shiny";
  }
}
