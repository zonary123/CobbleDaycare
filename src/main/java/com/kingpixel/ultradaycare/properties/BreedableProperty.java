package com.kingpixel.ultradaycare.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.ultradaycare.UltraDaycare;
import org.jetbrains.annotations.NotNull;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class BreedableProperty implements CustomPokemonProperty {
  private boolean value = true;

  public BreedableProperty(boolean s) {
    this.value = s;
  }


  @NotNull
  @Override
  public String asString() {
    if (this.value) {
      return "true";
    } else {
      return "false";
    }
  }

  @Override
  public void apply(@NotNull PokemonEntity pokemonEntity) {
    var pokemon = pokemonEntity.getPokemon();
    UltraDaycare.setBreedable(pokemon, this.value);
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !this.value);
  }

  @Override
  public void apply(@NotNull Pokemon pokemon) {
    UltraDaycare.setBreedable(pokemon, this.value);
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !this.value);
  }


  @Override
  public boolean matches(@NotNull Pokemon pokemon) {
    return UltraDaycare.isBreedable(pokemon) == this.value;
  }

  @Override
  public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return UltraDaycare.isBreedable(pokemonEntity.getPokemon()) == this.value;
  }
}
