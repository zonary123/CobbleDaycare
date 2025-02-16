package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

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
    incenses.add(new Incense("Full Incense", "Full Incense", List.of(""), 1, List.of(
      new PokemonIncense(
        "snorlax",
        "munchlax"
      )
    )));
    incenses.add(new Incense("Lax Incense", "Lax Incense", List.of(""), 2, List.of(
      new PokemonIncense(
        "wobbuffet",
        "wynaut"
      )
    )));
    incenses.add(new Incense("Sea Incense", "Sea Incense", List.of(""), 3, List.of(
      new PokemonIncense(
        "marill",
        "azurill"
      )
    )));
    incenses.add(new Incense("Rose Incense", "Rose Incense", List.of(""), 4, List.of(
      new PokemonIncense(
        "roselia",
        "budew"
      )
    )));
    incenses.add(new Incense("Pure Incese", "Pure Incese", List.of(""), 5, List.of(
      new PokemonIncense(
        "chimecho",
        "chingling"
      )
    )));
    incenses.add(new Incense("Rock Incense", "Rock Incense", List.of(""), 6, List.of(
      new PokemonIncense(
        "sudowoodo",
        "bonsly"
      )
    )));
    incenses.add(new Incense("Odd Incense", "Odd Incense", List.of(""), 7, List.of(
      new PokemonIncense(
        "mrmime",
        "mimejr"
      )
    )));
    incenses.add(new Incense("Luck Incense", "Luck Incense", List.of(""), 8, List.of(
      new PokemonIncense(
        "chansey",
        "happiny"
      )
    )));

    return incenses;
  }

  public static void giveIncense(ServerPlayerEntity player, String id) {
    Incense incense = null;
    for (Incense incense1 : CobbleDaycare.incenses) {
      if (incense1.getId().equals(id)) {
        incense = incense1;
        break;
      }
    }
    if (incense == null) return;
    ItemStack itemStack = incense.getItemStack();
    NbtCompound nbt = new NbtCompound();
    nbt.putString("id", incense.getId());
    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    player.getInventory().insertStack(itemStack);
  }


  public static boolean isIncense(ItemStack itemStack) {
    if (itemStack == null) return false;
    if (itemStack.getItem() == Items.AIR) return false;

    NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    if (nbtComponent == null) return false;
    NbtCompound nbt = nbtComponent.getNbt();
    if (nbt == null) return false;
    A
    return nbt.getString("id").equals(id);
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
