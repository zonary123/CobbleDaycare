package com.kingpixel.cobbleutils.features.breeding.models;

import com.kingpixel.cobbleutils.Model.PokemonData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Carlos Varas Alonso - 12/08/2024 12:38
 */
@Getter
@Setter
@Data
@ToString
public class PokemonIncense {
  private PokemonData parent;
  private PokemonData child;

  public PokemonIncense() {
    parent = new PokemonData("snorlax", "normal");
    child = new PokemonData("munchlax", "normal");
  }

  public PokemonIncense(PokemonData parent, PokemonData child) {
    this.parent = parent;
    this.child = child;
  }
}
