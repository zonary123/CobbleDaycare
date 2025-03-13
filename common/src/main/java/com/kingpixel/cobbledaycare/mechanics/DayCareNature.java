package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:57
 */
public class DayCareNature extends Mechanics {
  public static final String TAG_NATURE = "Nature";
  private float percentageEverstone;

  public DayCareNature() {
    this.percentageEverstone = 100F;
  }

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    if (hasNature(parents) && Utils.RANDOM.nextFloat() * 100 < percentageEverstone) {
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

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String s = egg.getPersistentData().getString(TAG_NATURE);
    if (!s.isEmpty()) {
      Nature nature = Natures.INSTANCE.getNature(s);
      if (nature == null) nature = Natures.INSTANCE.getRandomNature();
      egg.setNature(nature);
    } else {
      egg.setNature(Natures.INSTANCE.getRandomNature());
    }
    egg.getPersistentData().remove(TAG_NATURE);
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "nature";
  }

  @Override public String replace(String text) {
    return text
      .replace("%everstone%", String.format("%.2f", percentageEverstone));
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
