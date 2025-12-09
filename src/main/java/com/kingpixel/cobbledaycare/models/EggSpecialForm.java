package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:22
 */
@Setter
@Getter
public class EggSpecialForm {
  private String form;
  private List<String> pokemons;

  public EggSpecialForm(String form, List<String> pokemons) {
    this.form = form;
    this.pokemons = pokemons;
  }

  public String getForm(Pokemon pokemon) {
    String form = "";
    if (pokemons.contains(pokemon.getSpecies().showdownId())) {
      form = this.form;
    }
    return form;
  }
}
