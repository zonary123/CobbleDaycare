package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.cobblemon.mod.common.CobblemonItems.*;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DaycareIvs extends Mechanics {
  public static final List<Stats> stats =
    Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.ACCURACY && stats1 != Stats.EVASION).toList();
  private static final Map<Stats, String> oldStats = Map.of(
    Stats.HP, "HP",
    Stats.ATTACK, "Attack",
    Stats.DEFENCE, "Defense",
    Stats.SPECIAL_ATTACK, "SpecialAttack",
    Stats.SPECIAL_DEFENCE, "SpecialDefense",
    Stats.SPEED, "Speed"
  );

  private int defaultIvsTransfer;
  private int destinyKnotIvsTransfer;
  private int maxIvsRandom;
  private float percentagePowerItem;
  private float percentageDestinyKnot;

  public DaycareIvs() {
    this.defaultIvsTransfer = 3;
    this.destinyKnotIvsTransfer = 5;
    this.maxIvsRandom = 31;
    this.percentagePowerItem = 100f;
    this.percentageDestinyKnot = 100f;
  }

  @Override public String replace(String text) {
    return text
      .replace("%destinyknot%", String.format("%.2f", percentageDestinyKnot))
      .replace("%poweritem%", String.format("%.2f", percentagePowerItem))
      .replace("%maxivs%", String.valueOf(maxIvsRandom))
      .replace("%defaultIvsTransfer%", String.valueOf(defaultIvsTransfer))
      .replace("%destinyKnotIvsTransfer%", String.valueOf(destinyKnotIvsTransfer))
      .replace("%maxIvsRandom%", String.valueOf(maxIvsRandom));
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    List<Pokemon> parents = builder.getParents();
    Pokemon egg = builder.getEgg();
    List<Stats> cloneStats = new ArrayList<>(stats);
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "DaycareIvs -> applyEgg -> parents: " + cloneStats);
    }
    int numIvsToTransfer = getDefaultIvsTransfer();

    if (hasDestinyKnot(parents) && Utils.RANDOM.nextFloat() * 100 < getPercentageDestinyKnot()) {
      numIvsToTransfer = getDestinyKnotIvsTransfer();
    }
    // Power Items
    for (Pokemon parent : parents) {
      if (Utils.RANDOM.nextFloat() * 100 < getPercentagePowerItem())
        if (powerItem(parent, egg, cloneStats)) numIvsToTransfer--;
    }

    applyLastIvs(parents, egg, cloneStats, numIvsToTransfer);
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    stats.forEach(stat -> {
      int iv;
      String oldStat = oldStats.get(stat);
      if (egg.getPersistentData().contains(oldStat)) {
        iv = egg.getPersistentData().getInt(oldStat);
      } else {
        iv = egg.getPersistentData().getInt(stat.getShowdownId());
      }
      egg.getIvs().set(stat, iv);
      egg.getPersistentData().remove(stat.getShowdownId());
      egg.getPersistentData().remove(oldStat);
    });
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    stats.forEach(stat -> {
      int iv = pokemon.getIvs().getOrDefault(stat);
      egg.getPersistentData().putInt(stat.getShowdownId(), iv);
      egg.getIvs().set(stat, iv);
    });
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "ivs";
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
      int random = getMaxIvsRandom() + 1;
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
    } else {
      egg.getIvs().set(stat, 0);
    }
    egg.getPersistentData().putInt(stat.getShowdownId(), iv);
    cloneStats.remove(stat);
  }
}
