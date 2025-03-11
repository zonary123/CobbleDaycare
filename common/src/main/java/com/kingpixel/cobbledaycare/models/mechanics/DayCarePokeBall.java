package com.kingpixel.cobbledaycare.models.mechanics;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCarePokeBall extends Mechanics {
  public static final String TAG = "pokeball";

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    if (CobbleDaycare.config.isPokeBallFromMother()) {
      if (female.getSpecies().showdownId().equalsIgnoreCase(male.getSpecies().showdownId())) {
        if (Utils.RANDOM.nextBoolean()) {
          egg.setCaughtBall(male.getCaughtBall());
        } else {
          egg.setCaughtBall(female.getCaughtBall());
        }
      } else {
        egg.setCaughtBall(female.getCaughtBall());
      }
      Identifier pokeBall = female.getCaughtBall().getName();
      egg.getPersistentData().putString(TAG, pokeBall.getNamespace() + ":" + pokeBall.getPath());
    }
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String ball = egg.getPersistentData().getString(TAG);
    if (!ball.isEmpty()) {
      try {
        PokeBall pokeBall = PokeBalls.INSTANCE.getPokeBall(Identifier.of(ball));
        if (pokeBall != null) {
          egg.setCaughtBall(pokeBall);
        }
      } catch (Exception ignored) {
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.error("Error to get PokeBall: " + ball);
        }
      }
    }
    egg.getPersistentData().remove(TAG);
  }
}
