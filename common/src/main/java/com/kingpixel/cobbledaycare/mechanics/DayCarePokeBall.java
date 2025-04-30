package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbledaycare.models.HatchBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCarePokeBall extends Mechanics {
  public static final String TAG = "pokeball";
  private boolean viewPokeball;

  public DayCarePokeBall() {
    this.viewPokeball = true;
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Identifier id = builder.getFemale().getCaughtBall().getName();
    PokeBall pokeBall = PokeBalls.INSTANCE.getPokeBall(id);
    if (pokeBall != null) {
      if (viewPokeball)
        builder.getEgg().setCaughtBall(pokeBall);
    }
    builder.getEgg().getPersistentData().putString(TAG, id.getNamespace() + ":" + id.getPath());
  }

  @Override public String replace(String text, ServerPlayerEntity player) {
    return text
      .replace("%pokeball%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override public void applyHatch(HatchBuilder builder) {
    Pokemon egg = builder.getEgg();
    Pokemon pokemon = builder.getPokemon();
    String ball = egg.getPersistentData().getString(TAG);
    if (!ball.isEmpty()) {
      try {
        PokeBall pokeBall = PokeBalls.INSTANCE.getPokeBall(Identifier.of(ball));
        if (pokeBall != null) {
          pokemon.setCaughtBall(pokeBall);
        }
      } catch (Exception ignored) {
      }
    }
    egg.getPersistentData().remove(TAG);
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    Identifier id = pokemon.getCaughtBall().getName();
    egg.getPersistentData().putString(TAG, id.getNamespace() + ":" + id.getPath());
  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%pokeball%", nbt.getString(TAG));
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "pokeball";
  }
}
