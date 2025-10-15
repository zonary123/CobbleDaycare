package com.kingpixel.cobbledaycare.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import org.jetbrains.annotations.NotNull;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class BreedableProperty implements CustomPokemonProperty {
  private boolean value = true;

  public BreedableProperty(boolean s) {
    this.value = s;
  }


  @NotNull @Override public String asString() {
    if (this.value) {
      return "true";
    } else {
      return "false";
    }
  }

  @Override public void apply(@NotNull PokemonEntity pokemonEntity) {
    var pokemon = pokemonEntity.getPokemon();
    CobbleDaycare.setBreedable(pokemon, this.value);
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !this.value);
  }

  @Override public void apply(@NotNull Pokemon pokemon) {
    CobbleDaycare.setBreedable(pokemon, this.value);
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !this.value);
  }


  @Override public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  @Override public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return true;
  }
}
