package com.kingpixel.cobbledaycare.migrate;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OldPlot {
  private JsonObject male;
  private JsonObject female;
  private List<JsonObject> eggs;
  private long cooldown;

  public OldPlot() {
    male = null;
    female = null;
    eggs = new ArrayList<>();
    cooldown = 0;
  }

  public Plot toNewPlot() {
    Plot plot = new Plot();
    plot.setMale(male != null ? getPokemon(male) : null);
    plot.setFemale(female != null ? getPokemon(female) : null);
    List<Pokemon> eggList = new ArrayList<>();
    for (JsonObject egg : eggs) {
      eggList.add(getPokemon(egg));
    }
    plot.setEggs(eggList);
    plot.setTimeToHatch(cooldown);
    return plot;
  }

  private Pokemon getPokemon(JsonElement json) {
    Pokemon pokemon = null;
    try {
      pokemon = Pokemon.getCODEC().decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
      if (pokemon == null) CobbleUtils.LOGGER.info("Error deserializing pokemon");
    } catch (Exception e) {
      CobbleUtils.LOGGER.info("Error deserializing pokemon: " + e.getMessage());
    }
    return pokemon;
  }

}

