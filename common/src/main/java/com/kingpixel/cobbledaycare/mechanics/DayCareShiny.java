package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareShiny extends Mechanics {
  public static final String TAG = "shiny";
  private boolean active;
  private float percentageShiny;
  private float multiplierShiny;
  private float multiplierMasuda;

  public DayCareShiny() {
    this.active = true;
    this.percentageShiny = 8192;
  }


  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    float shinyrate = getPercentageShiny();
    float multiplier = getMultiplierShiny();

    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "DayCareShiny -> applyEgg -> shinyrate: " + shinyrate);
    }

    if (multiplier > 0) {
      if (male.getShiny())
        shinyrate /= multiplier;
      if (female.getShiny())
        shinyrate /= multiplier;
    }

    if (CobbleDaycare.config.getOptionsMecanics().isMasuda()) {
      String maleCountry = male.getPersistentData().getString(DayCareCountry.TAG);
      String femaleCountry = female.getPersistentData().getString(DayCareCountry.TAG);
      if (!maleCountry.isEmpty() && !femaleCountry.isEmpty()) {
        if (!maleCountry.equalsIgnoreCase(femaleCountry)) {
          shinyrate /= getMultiplierMasuda();
        }
      }
    }

    shinyrate = (int) Math.max(1, shinyrate);
    if (shinyrate <= 1) {
      egg.setShiny(true);
    } else {
      egg.setShiny(Utils.RANDOM.nextInt((int) shinyrate) == 0);
    }
    egg.getPersistentData().putString(TAG, Boolean.toString(egg.getShiny()));
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    boolean shiny = Boolean.parseBoolean(egg.getPersistentData().getString(TAG));
    egg.setShiny(shiny);
    egg.getPersistentData().remove(TAG);
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "shiny";
  }
}
