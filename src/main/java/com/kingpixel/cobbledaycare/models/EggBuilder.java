package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Builder;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 25/03/2025 5:39
 */
@Data
@Builder
public class EggBuilder {
  private ServerPlayerEntity player;
  private Pokemon male;
  private Pokemon female;
  private Pokemon egg;
  private List<Pokemon> parents;
  private Pokemon firstEvolution;

  public EggBuilder(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    this.player = player;
    this.male = male;
    this.female = female;
    this.egg = egg;
    this.parents = parents;
    this.firstEvolution = firstEvolution;
  }

}