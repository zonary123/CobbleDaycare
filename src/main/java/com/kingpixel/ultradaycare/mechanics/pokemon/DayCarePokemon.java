package com.kingpixel.ultradaycare.mechanics.pokemon;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.OriginalTrainerType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.Model.PokemonChance;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.mechanics.Mechanics;
import com.kingpixel.ultradaycare.models.EggBuilder;
import com.kingpixel.ultradaycare.models.HatchBuilder;
import com.kingpixel.ultradaycare.models.PokemonRareMecanic;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 11/03/2025 7:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DayCarePokemon extends Mechanics {
  public static final String TAG_OLD_POKEMON = "species";
  public static final String TAG_POKEMON = "pokemon";
  public static final String TAG_STEPS = "steps";
  public static final String TAG_REFERENCE_STEPS = "reference_steps";
  public static final String TAG_REFERENCE_CYCLES = "reference_cycles";
  public static final String TAG_CYCLES = "cycles";
  public static final String TAG_GENDER = "gender";
  private List<PokemonRareMecanic> pokemonRareMechanics;

  public DayCarePokemon() {
    this.pokemonRareMechanics = List.of(
      new PokemonRareMecanic(List.of(
        new PokemonChance("nidoranf", 50),
        new PokemonChance("nidoranm", 50))),
      new PokemonRareMecanic(List.of(
        new PokemonChance("illumise", 50),
        new PokemonChance("volbeat", 50))));
  }

  private static final Map<String, Integer> SPECIAL_FORM_INDEXES = Map.of(
    "sirfetch’d", 1,
    "sirfetch'd", 1,
    "cursola", 1,
    "obstagoon", 1,
    "runerigus", 1,
    "clodsire", 1,
    "overqwil", 1,
    "sneasler", 1,
    "perrserker", 2,
    "basculegion", 2
  );

  public static FormData determineChildForm(FormData parentForm) {
    if (parentForm == null) return null;
    Species baseSpecies = parentForm.getSpecies();

    while (baseSpecies.getPreEvolution() != null) {
      Species pre = baseSpecies.getPreEvolution().getSpecies();
      if (pre.showdownId().equalsIgnoreCase(baseSpecies.showdownId())) {
        break;
      }
      baseSpecies = pre;
    }

    String parentNameLower = parentForm.getSpecies().getName().toLowerCase();
    Integer formIdx = SPECIAL_FORM_INDEXES.get(parentNameLower);
    if (formIdx != null && baseSpecies.getForms().size() > formIdx) {
      return baseSpecies.getForms().get(formIdx);
    }

    String parentFormId = parentForm.formOnlyShowdownId();
    if (!parentFormId.isEmpty()) {
      for (FormData f : baseSpecies.getForms()) {
        String childFormId = f.formOnlyShowdownId();
        if (childFormId.contains(parentFormId) || parentFormId.contains(childFormId)) {
          return f;
        }
      }
    }

    return baseSpecies.getStandardForm();
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon male = builder.getMale();
    Pokemon female = builder.getFemale();
    Pokemon egg = builder.getEgg();
    Pokemon firstEvolution;
    egg.setOriginalTrainerType$common(OriginalTrainerType.PLAYER);
    egg.setOriginalTrainer(builder.getPlayer() != null ? builder.getPlayer().getUuid() : new UUID(0L, 0L));
    boolean maleIsDitto = male.getForm().getEggGroups().contains(EggGroup.DITTO);
    boolean femaleIsDitto = female.getForm().getEggGroups().contains(EggGroup.DITTO);
    boolean dobbleDitto = maleIsDitto && femaleIsDitto;

    if (dobbleDitto) {
      firstEvolution = UltraDaycare.config.getDobbleDittoFilter().generateRandomPokemon(UltraDaycare.MOD_ID, "egg");
    } else {
      if (femaleIsDitto) {
        // Swap male and female in the builder
        builder.setMale(female);
        builder.setFemale(male);
        male = builder.getMale();
        female = builder.getFemale();
      }
      firstEvolution = female;
      var incensePokemon = DayCareInciense.INSTANCE().applyIncense(firstEvolution);
      if (incensePokemon != null) {
        firstEvolution = incensePokemon;
      } else {
        firstEvolution = getEvolutionPokemonEgg(firstEvolution);
      }
    }

    builder.setFirstEvolution(firstEvolution);
    PokemonProperties.Companion.parse(getTypeEgg(firstEvolution)).apply(egg);
    double steps = UltraDaycare.config.getSteps(firstEvolution);
    egg.getPersistentData().putString(TAG_POKEMON, firstEvolution.getSpecies().showdownId());
    egg.getPersistentData().putDouble(TAG_STEPS, steps);
    egg.getPersistentData().putDouble(TAG_REFERENCE_STEPS, steps);
    egg.getPersistentData().putString(TAG_GENDER, firstEvolution.getGender().name());
    int cycles = firstEvolution.getSpecies().getEggCycles();
    egg.getPersistentData().putInt(TAG_CYCLES, cycles);
    egg.getPersistentData().putInt(TAG_REFERENCE_CYCLES, cycles);
  }

  private String getTypeEgg(Pokemon pokemon) {
    String showdownId = pokemon.showdownId();
    if (UltraDaycare.config.isDebug()) {
      UltraDaycare.LOGGER.info(UltraDaycare.MOD_ID, "type_egg=" + showdownId);
    }
    return "type_egg=" + showdownId;
  }

  @Override
  public void applyHatch(HatchBuilder builder) {
    Pokemon egg = builder.getEgg();
    egg.setOriginalTrainer(builder.getPlayer().getUuid());
    ServerPlayerEntity player = builder.getPlayer();
    String pokemon = egg.getPersistentData().getString(TAG_POKEMON);
    if (pokemon.isEmpty())
      pokemon = egg.getPersistentData().getString(TAG_OLD_POKEMON);
    if (pokemon.isEmpty()) {
      PlayerUtils.sendMessage(
        player,
        "Error: Pokemon not found in egg",
        UltraDaycare.language.getPrefix(),
        TypeMessage.CHAT);
      pokemon = "rattata";
    }
    builder.setPokemon(PokemonProperties.Companion.parse(pokemon).create());
    String genderString = egg.getPersistentData().getString(TAG_GENDER);
    try {
      if (!genderString.isEmpty()) {
        builder.getPokemon().setGender(Gender.valueOf(genderString));
      }
    } catch (IllegalArgumentException e) {
      UltraDaycare.LOGGER.error("Invalid gender string on hatch: ", e);
    }

    egg.getPersistentData().remove(TAG_POKEMON);
    egg.getPersistentData().remove(TAG_OLD_POKEMON);
    egg.getPersistentData().remove(TAG_STEPS);
    egg.getPersistentData().remove(TAG_REFERENCE_STEPS);
    egg.getPersistentData().remove(TAG_CYCLES);
    egg.getPersistentData().remove(TAG_GENDER);
    egg.getPersistentData().remove(TAG_REFERENCE_CYCLES);
  }

  @Override
  public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    egg.setOriginalTrainerType$common(OriginalTrainerType.PLAYER);
    egg.setOriginalTrainer(player != null ? player.getUuid() : new UUID(0L, 0L));
    PokemonProperties.Companion.parse(getTypeEgg(pokemon)).apply(egg);
    egg.getPersistentData().putString(TAG_POKEMON, pokemon.getSpecies().showdownId());
    egg.getPersistentData().putDouble(TAG_STEPS, UltraDaycare.config.getSteps(pokemon));
    egg.getPersistentData().putDouble(TAG_REFERENCE_STEPS, UltraDaycare.config.getSteps(pokemon));
    egg.getPersistentData().putString(TAG_GENDER, pokemon.getGender().name());
    egg.getPersistentData().putInt(TAG_CYCLES, pokemon.getSpecies().getEggCycles());
    egg.getPersistentData().putInt(TAG_REFERENCE_CYCLES, pokemon.getSpecies().getEggCycles());
  }

  @Override
  public String getEggInfo(String s, NbtCompound nbt) {
    return s
      .replace("%steps%", String.format("%.2f", nbt.getDouble(TAG_STEPS)))
      .replace("%reference_steps%", String.format("%.2f", nbt.getDouble(TAG_REFERENCE_STEPS)))
      .replace("%cycles%", String.valueOf(nbt.getInt(TAG_CYCLES)))
      .replace("%pokemon%", nbt.getString(TAG_POKEMON))
      .replace("%gender%", PokemonUtils.getGenderTranslate(Gender.valueOf(nbt.getString(TAG_GENDER))));
  }

  @Override
  public void validateData() {
  }

  @Override
  public String fileName() {
    return "pokemon";
  }

  @Override
  public String replace(String text, ServerPlayerEntity player) {
    return text;
  }

  public Pokemon createChildPokemonInstance(Pokemon pokemon) {
    if (pokemon == null) return null;
    return createChildPokemonInstance(pokemon.getForm());
  }

  public Pokemon createChildPokemonInstance(FormData form) {
    if (form == null) return null;
    Species species = form.getSpecies();
    if (species.showdownId().equalsIgnoreCase("manaphy")) {
      return PokemonSpecies.getByIdentifier(Identifier.of("cobblemon:phione")).create(1);
    }
    FormData childForm = determineChildForm(form);
    Pokemon specialPokemon = findSpecialPokemon(childForm.getSpecies());
    if (specialPokemon != null) return specialPokemon;

    Pokemon child = childForm.getSpecies().create(1);
    child.setForm(childForm);
    return child;
  }

  public Pokemon getEvolutionPokemonEgg(Pokemon pokemon) {
    return createChildPokemonInstance(pokemon);
  }

  public Pokemon getEvolutionPokemonEgg(FormData form) {
    return createChildPokemonInstance(form);
  }

  public Pokemon getEvolutionPokemonEgg(Species species) {
    if (species == null) return null;
    return createChildPokemonInstance(species.getStandardForm());
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
