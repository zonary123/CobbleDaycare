package com.kingpixel.cobbledaycare.models;

import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 1:17
 */
public class Plot {
  private List<JsonObject> parents;
  private List<JsonObject> eggs;
  private boolean creatingEgg;
  private long timeToHatch;

  public List<Pokemon> getParents() {
    List<Pokemon> pokemons = new ArrayList<>();
    for (JsonObject parent : parents) {
      if (parent == null) continue;
      pokemons.add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, parent));
    }
    return pokemons.stream().toList();
  }

  public List<Pokemon> getEggs() {
    List<Pokemon> eggs = new ArrayList<>();
    for (JsonObject egg : this.eggs) {
      if (egg == null) continue;
      eggs.add(Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, egg));
    }
    return eggs;
  }


  public boolean giveEggs(ServerPlayerEntity player) {
    boolean update = false;
    List<JsonObject> remove = new ArrayList<>();
    for (JsonObject egg : eggs) {
      if (egg == null) continue;
      Pokemon pokemon = Pokemon.Companion.loadFromJSON(DynamicRegistryManager.EMPTY, egg);
      Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
      remove.add(egg);
    }
    if (!remove.isEmpty()) {
      eggs.removeAll(remove);
      update = true;
    }
    return update;
  }


  private void openPc(ServerPlayerEntity player, UserInformation userInformation) {
    ChestTemplate template = ChestTemplate
      .builder(6)
      .build();


  }

  public Pokemon getMale() {
  }

  public boolean canCreateEgg() {

  }
}
