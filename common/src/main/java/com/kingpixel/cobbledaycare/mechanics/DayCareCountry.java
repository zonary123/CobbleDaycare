package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public class DayCareCountry extends Mechanics {
  public static final String TAG = "country";

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    var countryInfo = CobbleDaycare.getCountry(player);
    if (countryInfo == null) return;
    egg.getPersistentData().putString(TAG, countryInfo.country());
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
  }

  @Override public void commandCreateEgg(ServerPlayerEntity player, Pokemon pokemon) {

  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "country";
  }

  @Override public String replace(String text) {
    return text;
  }
}
