package com.kingpixel.cobbledaycare.properties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import org.jetbrains.annotations.NotNull;

/**
 * @author Carlos Varas Alonso - 04/08/2024 19:40
 */
public class BreedableProperty implements CustomPokemonProperty {
  private boolean value;

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
    CobbleDaycare.setBreedable(pokemonEntity.getPokemon(), this.value);
  }

  @Override public void apply(@NotNull Pokemon pokemon) {
    CobbleDaycare.setBreedable(pokemon, this.value);
  }


  @Override public boolean matches(@NotNull Pokemon pokemon) {
    return true;
  }

  @Override public boolean matches(@NotNull PokemonEntity pokemonEntity) {
    return true;
  }
}
