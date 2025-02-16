package com.kingpixel.cobbledaycare.models.mecanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.Utils;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public class DayCareShiny extends Mechanics {

  public void applyEgg(List<Pokemon> parents, Pokemon egg) {
    if (!egg.getShiny()) egg.setShiny(Utils.RANDOM.nextInt(2048) == 0);
  }

  public void applyHatch(Pokemon pokemon) {
  }
}
