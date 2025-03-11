package com.kingpixel.cobbledaycare.models.mechanics;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public class DayCareShiny extends Mechanics {

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    float shinyrate = Cobblemon.INSTANCE.getConfig().getShinyRate();
    float multiplier = CobbleDaycare.config.getMultiplierShiny();

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
          shinyrate /= CobbleDaycare.config.getMultipliermasuda();
        }
      }
    }

    shinyrate = (int) Math.max(1, shinyrate);
    if (shinyrate <= 1) {
      egg.setShiny(true);
    } else {
      egg.setShiny(Utils.RANDOM.nextInt((int) shinyrate) == 0);
    }
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
  }
}
