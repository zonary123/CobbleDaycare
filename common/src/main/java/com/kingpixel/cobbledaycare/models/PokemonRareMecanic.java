package com.kingpixel.cobbledaycare.models;

import com.kingpixel.cobbleutils.Model.PokemonChance;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:22
 */
@Getter
@Setter
public class PokemonRareMecanic {
  private List<PokemonChance> pokemons;

  public PokemonRareMecanic(List<PokemonChance> pokemons) {
    this.pokemons = pokemons;
  }
}
