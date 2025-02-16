package com.kingpixel.cobbledaycare.models;


import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.mecanics.DaycareNature;
import com.kingpixel.cobbledaycare.models.mecanics.Mechanics;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.nbt.NbtCompound;

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
  private static final String TAG_Cycles = "Cycles";
  private static final String TAG_Steps = "Steps";
  private static final List<Stats> stats = new ArrayList<>(Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.EVASION && stats1 != Stats.ACCURACY).toList());
  private String species;
  private int level;
  private double steps;
  private int cycles;
  private String ability;
  private String size;
  private String form;
  private String moves;
  private String ball;
  private String nature;
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
    eggData.setCycles(nbt.getInt(TAG_Cycles));
    eggData.setSteps(nbt.getDouble(TAG_Steps));
    eggData.setHP(nbt.getInt(Stats.HP.getShowdownId()));
    eggData.setAttack(nbt.getInt(Stats.ATTACK.getShowdownId()));
    eggData.setDefense(nbt.getInt(Stats.DEFENCE.getShowdownId()));
    eggData.setSpecialAttack(nbt.getInt(Stats.SPECIAL_ATTACK.getShowdownId()));
    eggData.setSpecialDefense(nbt.getInt(Stats.SPECIAL_DEFENCE.getShowdownId()));
    eggData.setSpeed(nbt.getInt(Stats.SPEED.getShowdownId()));
    eggData.setNature(nbt.getString(DaycareNature.TAG_NATURE));
    return eggData;
  }

  public static void a() {
    Pokemon egg = PokemonProperties.Companion.parse("egg").create();
    for (Mechanics mechanic : CobbleDaycare.mechanics) {
      mechanic.applyEgg(new ArrayList<>(), egg);
    }

  }


}