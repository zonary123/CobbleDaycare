package com.kingpixel.cobbledaycare.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class EggForm {
  private String form;
  private List<String> pokemons;

  public EggForm(String form, List<String> pokemons) {
    this.form = form;
    this.pokemons = pokemons;
  }
}
