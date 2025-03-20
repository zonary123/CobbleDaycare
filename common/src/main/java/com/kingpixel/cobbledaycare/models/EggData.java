package com.kingpixel.cobbledaycare.models;


import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.events.HatchEggEvent;
import com.kingpixel.cobbledaycare.mechanics.*;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 23/07/2024 23:01
 */
@Getter
@Setter
@ToString
public class EggData {
  private static final List<Stats> stats = new ArrayList<>(Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.EVASION && stats1 != Stats.ACCURACY).toList());
  private String pokemon;
  private int level;
  private double steps;
  private int cycles;
  private String ability;
  private String size;
  private String form;
  private String moves;
  private String ball;
  private String nature;
  private String country;
  // Ivs
  private int HP;
  private int Attack;
  private int Defense;
  private int SpecialAttack;
  private int SpecialDefense;
  private int Speed;


  public static EggData from(Pokemon pokemon) {
    if (pokemon == null) return null;
    EggData eggData = new EggData();
    NbtCompound nbt = pokemon.getPersistentData();
    eggData.setPokemon(nbt.getString(DayCarePokemon.TAG_POKEMON));
    eggData.setCycles(nbt.getInt(DayCarePokemon.TAG_CYCLES));
    eggData.setSteps(nbt.getDouble(DayCarePokemon.TAG_STEPS));
    eggData.setHP(nbt.getInt(Stats.HP.getShowdownId()));
    eggData.setAttack(nbt.getInt(Stats.ATTACK.getShowdownId()));
    eggData.setDefense(nbt.getInt(Stats.DEFENCE.getShowdownId()));
    eggData.setSpecialAttack(nbt.getInt(Stats.SPECIAL_ATTACK.getShowdownId()));
    eggData.setSpecialDefense(nbt.getInt(Stats.SPECIAL_DEFENCE.getShowdownId()));
    eggData.setSpeed(nbt.getInt(Stats.SPEED.getShowdownId()));
    eggData.setNature(nbt.getString(DayCareNature.TAG_NATURE));
    eggData.setBall(nbt.getString(DayCarePokeBall.TAG));
    eggData.setMoves(nbt.getString(DayCareEggMoves.TAG));
    eggData.setCountry(nbt.getString(DayCareCountry.TAG));
    eggData.setAbility(nbt.getString(DayCareAbility.TAG));
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

  public void sendEggInfo(ServerPlayerEntity player) {
    PlayerUtils.sendMessage(
      player,
      toString(),
      CobbleDaycare.language.getPrefix(),
      TypeMessage.CHAT
    );
  }
}