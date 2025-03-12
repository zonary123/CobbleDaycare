package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 7:38
 */
public class DayCarePokemon extends Mechanics {
  public static final String TAG_OLD_POKEMON = "species";
  public static final String TAG_POKEMON = "pokemon";
  public static final String TAG_STEPS = "steps";
  public static final String TAG_REFERENCE_STEPS = "reference_steps";
  public static final String TAG_CYCLES = "cycles";


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
        firstEvolution = PokemonUtils.getEvolutionPokemonEgg(firstEvolution.getSpecies());
      } else {
        firstEvolution = PokemonProperties.Companion.parse(lure_species).create();
      }
    }

    double steps = CobbleDaycare.config.getSteps(firstEvolution);
    egg.getPersistentData().putString(TAG_POKEMON, firstEvolution.getSpecies().showdownId());
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
}
