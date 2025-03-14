package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.PokemonRareMecanic;
import com.kingpixel.cobbleutils.Model.PokemonChance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Carlos Varas Alonso - 11/03/2025 7:38
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCarePokemon extends Mechanics {
  public static final String TAG_OLD_POKEMON = "species";
  public static final String TAG_POKEMON = "pokemon";
  public static final String TAG_STEPS = "steps";
  public static final String TAG_REFERENCE_STEPS = "reference_steps";
  public static final String TAG_CYCLES = "cycles";
  private List<PokemonRareMecanic> pokemonRareMechanics;

  public DayCarePokemon() {
    this.pokemonRareMechanics = List.of(
      new PokemonRareMecanic(List.of(
        new PokemonChance("nidoranf", 50),
        new PokemonChance("nidoranm", 50)
      )),
      new PokemonRareMecanic(List.of(
        new PokemonChance("illumise", 50),
        new PokemonChance("volbeat", 50)
      ))
    );
  }

  private static Species getFirstPreEvolution(Species species) {
    while (species.getPreEvolution() != null) {
      Species preEvolution = species.getPreEvolution().getSpecies();

      // Si encontramos un bucle en la cadena evolutiva, rompemos el ciclo
      if (preEvolution.showdownId().equalsIgnoreCase(species.showdownId())) {
        break;
      }

      species = preEvolution;
    }

    return species;
  }

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    boolean maleIsDitto = male.getForm().getEggGroups().contains(EggGroup.DITTO);
    boolean femaleIsDitto = female.getForm().getEggGroups().contains(EggGroup.DITTO);
    boolean dobbleDitto = maleIsDitto && femaleIsDitto;
    if (dobbleDitto) {
      firstEvolution = CobbleDaycare.config.getDobbleDittoFilter().generateRandomPokemon(CobbleDaycare.MOD_ID, "egg");
    } else {
      if (femaleIsDitto) {
        Pokemon temp = female;
        female = male;
        male = temp;
      }
      firstEvolution = female;
      String lure_species = null;
      if (lure_species == null) {
        firstEvolution = getEvolutionPokemonEgg(firstEvolution.getSpecies());
      } else {
        firstEvolution = PokemonProperties.Companion.parse(lure_species).create();
      }
    }

    double steps = CobbleDaycare.config.getSteps(firstEvolution);
    egg.getPersistentData().putString(TAG_POKEMON, firstEvolution.getSpecies().showdownId());
    egg.getPersistentData().putString(TAG_OLD_POKEMON, firstEvolution.getSpecies().showdownId());
    egg.getPersistentData().putDouble(TAG_STEPS, steps);
    egg.getPersistentData().putDouble(TAG_REFERENCE_STEPS, steps);
    egg.getPersistentData().putInt(TAG_CYCLES, firstEvolution.getSpecies().getEggCycles());
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String pokemon = egg.getPersistentData().getString(TAG_POKEMON);
    if (pokemon.isEmpty()) pokemon = egg.getPersistentData().getString(TAG_OLD_POKEMON);
    PokemonProperties.Companion.parse(pokemon).apply(egg);
    egg.heal();
    egg.getPersistentData().remove(TAG_POKEMON);
    egg.getPersistentData().remove(TAG_OLD_POKEMON);
    egg.getPersistentData().remove(TAG_STEPS);
    egg.getPersistentData().remove(TAG_REFERENCE_STEPS);
    egg.getPersistentData().remove(TAG_CYCLES);
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "pokemon";
  }

  public Pokemon getEvolutionPokemonEgg(Pokemon pokemon) {
    return getEvolutionPokemonEgg(pokemon.getSpecies());
  }

  public Pokemon getEvolutionPokemonEgg(Species species) {
    if (species.showdownId().equals("manaphy"))
      return PokemonSpecies.INSTANCE.getByIdentifier(Identifier.of("cobblemon:phione")).create(1);
    Species firstEvolution = getFirstPreEvolution(species);

    Pokemon specialPokemon = findSpecialPokemon(firstEvolution);

    // Usamos Objects.requireNonNullElseGet para devolver el Pokémon especial si existe, o crear uno nuevo si no
    return Objects.requireNonNullElseGet(specialPokemon, () -> firstEvolution.create(1));
  }

  private Pokemon findSpecialPokemon(Species species) {
    List<PokemonChance> specialPokemons = new ArrayList<>();

    for (PokemonRareMecanic pokemonRareMechanic : getPokemonRareMechanics()) {
      for (PokemonChance pokemon : pokemonRareMechanic.getPokemons()) {
        if (pokemon.getPokemon().equalsIgnoreCase(species.showdownId())) {
          specialPokemons = pokemonRareMechanic.getPokemons();
          break;
        }
      }
    }


    return PokemonChance.getPokemonCreate(specialPokemons);
  }

}
