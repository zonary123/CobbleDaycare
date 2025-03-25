package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCarePokeBall extends Mechanics {
  public static final String TAG = "pokeball";

  @Override
  public void applyEgg(EggBuilder builder) {
    Identifier id = builder.getFemale().getCaughtBall().getName();
    builder.getEgg().getPersistentData().putString(TAG, id.getNamespace() + ":" + id.getPath());
  }

  @Override public String replace(String text) {
    return text
      .replace("%pokeball%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
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

  @Override public void commandCreateEgg(ServerPlayerEntity player, Pokemon pokemon) {

  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "pokeball";
  }
}
