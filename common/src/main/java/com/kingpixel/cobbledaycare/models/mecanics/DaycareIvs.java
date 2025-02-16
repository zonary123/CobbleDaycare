package com.kingpixel.cobbledaycare.models.mecanics;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cobblemon.mod.common.CobblemonItems.*;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public class DaycareIvs extends Mechanics {
  private static final List<Stats> stats =
    Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.ACCURACY && stats1 != Stats.EVASION).toList();

  @Override public void applyEgg(List<Pokemon> parents, Pokemon egg) {
    List<Stats> cloneStats = new ArrayList<>(stats);
    int numIvsToTransfer = CobbleDaycare.config.getOptionsMecanics().getDefaultIvsTransfer();

    if (hasDestinyKnot(parents)) {
      numIvsToTransfer = CobbleDaycare.config.getOptionsMecanics().getDestinyKnotIvsTransfer();
    }
    // Power Items
    for (Pokemon parent : parents) {
      if (powerItem(parent, egg, cloneStats)) numIvsToTransfer--;
    }

    applyLastIvs(parents, egg, cloneStats, numIvsToTransfer);
  }

  @Override public void applyHatch(Pokemon pokemon) {
    stats.forEach(stat -> {
      int iv = pokemon.getIvs().getOrDefault(stat);
      pokemon.getPersistentData().putInt(stat.getShowdownId(), iv);
    });
  }

  private boolean hasDestinyKnot(List<Pokemon> parents) {
    for (Pokemon parent : parents) {
      if (parent.heldItem().getItem().equals(DESTINY_KNOT)) {
        return true;
      }
    }
    return false;
  }

  private void applyLastIvs(List<Pokemon> parents, Pokemon egg, List<Stats> stats, int numIvsToTransfer) {
    for (int i = 0; i < numIvsToTransfer; i++) {
      Stats stat = stats.get(Utils.RANDOM.nextInt(stats.size()));
      Pokemon parent = parents.get(Utils.RANDOM.nextInt(parents.size()));
      applyIvs(parent, egg, stat, stats);
    }
    stats.forEach(stat -> {
      int random = CobbleDaycare.config.getOptionsMecanics().getMaxIvsRandom() + 1;
      if (random < 0 || random > 31) random = 31;
      int iv = Utils.RANDOM.nextInt(random + 1);
      egg.getPersistentData().putInt(stat.getShowdownId(), iv);
      egg.getIvs().set(stat, iv);
    });
  }

  private boolean powerItem(Pokemon pokemon, Pokemon egg, List<Stats> cloneStats) {
    if (pokemon.heldItem().getItem() instanceof CobblemonItem item) {
      if (item == POWER_WEIGHT) {
        applyIvs(pokemon, egg, Stats.HP, cloneStats);
        return true;
      } else if (item == POWER_BRACER) {
        applyIvs(pokemon, egg, Stats.ATTACK, cloneStats);
        return true;
      } else if (item == POWER_BELT) {
        applyIvs(pokemon, egg, Stats.DEFENCE, cloneStats);
        return true;
      } else if (item == POWER_ANKLET) {
        applyIvs(pokemon, egg, Stats.SPEED, cloneStats);
        return true;
      } else if (item == POWER_LENS) {
        applyIvs(pokemon, egg, Stats.SPECIAL_ATTACK, cloneStats);
        return true;
      } else if (item == POWER_BAND) {
        applyIvs(pokemon, egg, Stats.SPECIAL_DEFENCE, cloneStats);
        return true;
      }
    }
    return false;
  }

  private void applyIvs(Pokemon pokemon, Pokemon egg, Stats stat, List<Stats> cloneStats) {
    int iv = pokemon.getIvs().getOrDefault(stat);
    if (CobbleDaycare.config.isShowIvs()) {
      egg.getIvs().set(stat, iv);
    }
    pokemon.getPersistentData().putInt(stat.getShowdownId(), iv);
    cloneStats.remove(stat);
  }
}
