package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.mechanics.DayCareInciense;
import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 12/08/2024 12:37
 */
@Getter
@Setter
@ToString
public class Incense extends ItemModel {
  private String id;
  private List<PokemonIncense> pokemonIncense;

  public Incense() {
    super("minecraft:emerald", "Full Incense", List.of(""), 1);
    this.id = "full_incense";
    pokemonIncense = new ArrayList<>();
    pokemonIncense.add(new PokemonIncense("", ""));
  }

  public Incense(String id, String displayname, List<String> lore, int custommodeldata,
                 List<PokemonIncense> pokemonIncense) {
    super("minecraft:emerald", displayname, lore, custommodeldata);
    this.id = id;
    this.pokemonIncense = pokemonIncense;
  }

  public static List<Incense> defaultIncenses() {
    List<Incense> incenses = new ArrayList<>();
    incenses.add(new Incense("fullincense", "Full Incense", List.of(""), 1, List.of(
      new PokemonIncense(
        "snorlax",
        "munchlax"
      )
    )));
    incenses.add(new Incense("laxincense", "Lax Incense", List.of(""), 2, List.of(
      new PokemonIncense(
        "wobbuffet",
        "wynaut"
      )
    )));
    incenses.add(new Incense("seaincense", "Sea Incense", List.of(""), 3, List.of(
      new PokemonIncense(
        "marill",
        "azurill"
      )
    )));
    incenses.add(new Incense("roseincense", "Rose Incense", List.of(""), 4, List.of(
      new PokemonIncense(
        "roselia",
        "budew"
      )
    )));
    incenses.add(new Incense("pureincense", "Pure Incese", List.of(""), 5, List.of(
      new PokemonIncense(
        "chimecho",
        "chingling"
      )
    )));
    incenses.add(new Incense("rockincense", "Rock Incense", List.of(""), 6, List.of(
      new PokemonIncense(
        "sudowoodo",
        "bonsly"
      )
    )));
    incenses.add(new Incense("oddincense", "Odd Incense", List.of(""), 7, List.of(
      new PokemonIncense(
        "mrmime",
        "mimejr"
      )
    )));
    incenses.add(new Incense("luckincense", "Luck Incense", List.of(""), 8, List.of(
      new PokemonIncense(
        "chansey",
        "happiny"
      )
    )));

    return incenses;
  }

  public static boolean isIncense(ItemStack itemStack) {
    if (itemStack == null) return false;
    if (itemStack.getItem() == Items.AIR) return false;

    NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    if (nbtComponent == null) return false;
    NbtCompound nbt = nbtComponent.getNbt();
    if (nbt == null) return false;
    return !nbt.getString(DayCareInciense.TAG_INCENSE).isEmpty();
  }

  public ItemStack getItemStackIncense(int amount) {
    ItemStack itemStack = super.getItemStack(amount);
    var nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    if (nbtComponent == null) {
      nbtComponent = NbtComponent.DEFAULT;
    }
    var nbt = nbtComponent.getNbt();
    if (nbt == null) {
      nbt = new NbtCompound();
    }
    nbt.putString(DayCareInciense.TAG_INCENSE, this.id);
    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    return itemStack;
  }

  public String getChild(Pokemon pokemon) {
    if (pokemonIncense.isEmpty()) return null;
    String pokemonName = pokemon.showdownId();

    if (isIncense(pokemon.heldItem())) {
      for (PokemonIncense pokemonIncense1 : pokemonIncense) {
        if (pokemonIncense1.getParent().equals(pokemonName)) {
          return pokemonIncense1.getChild();
        }
      }
    } else {
      for (PokemonIncense pokemonIncense1 : pokemonIncense) {
        if (pokemonIncense1.getParent().equals(pokemonName)) {
          return pokemonIncense1.getParent();
        }
      }
    }

    return null;
  }

}
