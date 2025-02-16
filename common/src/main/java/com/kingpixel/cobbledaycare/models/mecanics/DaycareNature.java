package com.kingpixel.cobbledaycare.models.mecanics;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:57
 */
public class DaycareNature extends Mechanics {
  public static final String TAG_NATURE = "Nature";

  @Override public void applyEgg(List<Pokemon> parents, Pokemon egg) {
    if (hasNature(parents)) {
      for (Pokemon parent : parents) {
        if (parent.heldItem().getItem() instanceof CobblemonItem item) {
          if (item.equals(CobblemonItems.EVERSTONE)) {
            applyNature(parent.getNature(), egg);
            break;
          }
        }
      }
    } else {
      applyNature(Natures.INSTANCE.getRandomNature(), egg);
    }
  }

  @Override public void applyHatch(Pokemon pokemon) {
    String s = pokemon.getPersistentData().getString(TAG_NATURE);
    if (!s.isEmpty()) {
      Nature nature = Natures.INSTANCE.getNature(s);
      if (nature == null) nature = Natures.INSTANCE.getRandomNature();
      pokemon.setNature(nature);
    } else {
      pokemon.setNature(Natures.INSTANCE.getRandomNature());
    }
    pokemon.getPersistentData().remove(TAG_NATURE);
  }

  private boolean hasNature(List<Pokemon> parents) {
    for (Pokemon parent : parents) {
      if (parent.heldItem().getItem() instanceof CobblemonItem item) {
        if (item.equals(CobblemonItems.EVERSTONE)) return true;
      }
    }
    return false;
  }

  private void applyNature(Nature nature, Pokemon egg) {
    egg.setNature(nature);
    egg.getPersistentData().putString(TAG_NATURE, nature.getName().getPath());
  }
}
