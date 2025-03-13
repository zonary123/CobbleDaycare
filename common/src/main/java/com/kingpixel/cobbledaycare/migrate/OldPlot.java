package com.kingpixel.cobbledaycare.migrate;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.kingpixel.cobbledaycare.models.Plot;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.registry.DynamicRegistryManager;

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
    plot.setMale(male != null ? Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, male) : null);
    plot.setFemale(female != null ? Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, female) : null);
    List<Pokemon> eggList = new ArrayList<>();
    for (JsonObject egg : eggs) {
      eggList.add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, egg));
    }
    plot.setEggs(eggList);
    plot.setTimeToHatch(cooldown);
    return plot;
  }


}

