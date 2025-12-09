package com.kingpixel.cobbledaycare.models;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Carlos Varas Alonso - 31/01/2025 2:55
 */
@Getter
@Setter
public class PokemonIncense {
  private String parent;
  private String child;

  public PokemonIncense(String parent, String child) {
    this.parent = parent;
    this.child = child;
  }
}
