package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
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
    Identifier id;
    if (CobbleDaycare.config.isPokeBallFromMother()) {
      id = female.getCaughtBall().getName();
      egg.getPersistentData().putString(TAG, id.getNamespace() + ":" + id.getPath());
    } else {
      id = PokeBalls.INSTANCE.getPOKE_BALL().getName();
      egg.getPersistentData().putString(TAG, id.getNamespace() + ":" + id.getPath());
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

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "pokeball";
  }
}
