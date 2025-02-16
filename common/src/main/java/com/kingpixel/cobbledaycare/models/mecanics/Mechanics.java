package com.kingpixel.cobbledaycare.models.mecanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public abstract class Mechanics {

  public void applyEgg(List<Pokemon> parents, Pokemon egg) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Not Implemented -> " + this.getClass().getName());
  }

  public void applyHatch(Pokemon pokemon) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Not Implemented -> " + this.getClass().getName());
  }
}
