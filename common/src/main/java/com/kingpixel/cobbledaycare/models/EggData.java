package com.kingpixel.cobbledaycare.models;


import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.events.HatchEggEvent;
import com.kingpixel.cobbledaycare.mechanics.DayCareForm;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * @author Carlos Varas Alonso - 23/07/2024 23:01
 */
@Getter
@Setter
@ToString
public class EggData {
  private String pokemon;
  private String form;
  private double steps;
  private int cycles;


  public static EggData from(Pokemon pokemon) {
    if (pokemon == null) return null;
    EggData eggData = new EggData();
    NbtCompound nbt = pokemon.getPersistentData();
    eggData.setPokemon(nbt.getString(DayCarePokemon.TAG_POKEMON));
    eggData.setCycles(nbt.getInt(DayCarePokemon.TAG_CYCLES));
    eggData.setSteps(nbt.getDouble(DayCarePokemon.TAG_STEPS));
    eggData.setForm(nbt.getString(DayCareForm.TAG));
    return eggData;
  }


  public void steps(ServerPlayerEntity player, Pokemon egg, double deltaMovement, UserInformation userInformation) {
    steps -= deltaMovement * userInformation.getActualMultiplier();
    egg.setCurrentHealth(0);
    if (steps <= 0) {
      steps = egg.getPersistentData().getDouble(DayCarePokemon.TAG_REFERENCE_STEPS);
      cycles--;
    }
    if (cycles <= 0) {
      hatch(player, egg);
      return;
    } else {
      egg.getPersistentData().putDouble(DayCarePokemon.TAG_STEPS, steps);
      egg.getPersistentData().putInt(DayCarePokemon.TAG_CYCLES, cycles);
    }
    updateName(egg);
  }

  private void updateName(Pokemon egg) {
    egg.setNickname(Text.literal(
      CobbleDaycare.language.getEggName()
        .replace("%steps%", String.format("%.2f", steps))
        .replace("%cycles%", String.valueOf(cycles))
        .replace("%pokemon%", pokemon)
    ));
  }

  public void hatch(ServerPlayerEntity player, Pokemon egg) {
    for (Mechanics mechanic : CobbleDaycare.mechanics) {
      mechanic.applyHatch(player, egg);
    }
    egg.setNickname(null);
    egg.heal();
    HatchEggEvent.HATCH_EGG_EVENT.emit(player, egg);
    PokemonProperties pokemonProperties = PokemonProperties.Companion.parse(pokemon + " " + form);
    CobblemonEvents.HATCH_EGG_POST.emit(new com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent.Post(pokemonProperties, player));
  }

}