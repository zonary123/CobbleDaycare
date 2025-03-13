package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareAbility extends Mechanics {
  public static final String TAG = "ability";
  private double percentageTransmitAH;

  public DayCareAbility() {
    this.percentageTransmitAH = 60;
  }

  private static boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getSpecies().showdownId().equalsIgnoreCase("ditto") ||
      pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  @Override public String replace(String text) {
    return text
      .replace("%ability%", String.format("%.2f", percentageTransmitAH))
      .replace("%activeAbility%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents,
                       Pokemon firstEvolution) {
    boolean hasAh = PokemonUtils.isAH(male) || PokemonUtils.isAH(female);
    boolean getAh = Utils.RANDOM.nextDouble(100) <= percentageTransmitAH;
    boolean notDitto = isDitto(male) || isDitto(female);

    if (getAh && hasAh && !notDitto) {
      egg.getPersistentData().putString(TAG, PokemonUtils.getAH(firstEvolution).getName());
    } else {
      egg.getPersistentData().putString(TAG, PokemonUtils.getRandomAbility(firstEvolution).getName());
    }

  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String ability = egg.getPersistentData().getString(TAG);
    AbilityTemplate abilityTemplate;
    AbilityTemplate randomAbility =
      PokemonUtils.getRandomAbility(egg).getTemplate();
    if (ability.isEmpty()) {
      abilityTemplate = randomAbility;
    } else {
      abilityTemplate = Abilities.INSTANCE.get(ability);
    }
    if (abilityTemplate == null) abilityTemplate = randomAbility;

    PokemonProperties.Companion.parse(abilityTemplate.getName()).apply(egg);
    egg.getPersistentData().remove(TAG);
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "ability";
  }
}
