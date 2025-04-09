package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public class DayCareCountry extends Mechanics {
  public static final String TAG = "country";

  @Override
  public void applyEgg(EggBuilder builder) {
    var countryInfo = CobbleDaycare.getCountry(builder.getPlayer());
    if (countryInfo == null) return;
    builder.getEgg().getPersistentData().putString(TAG, countryInfo.country());
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {

  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%country%", nbt.getString(TAG));
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
