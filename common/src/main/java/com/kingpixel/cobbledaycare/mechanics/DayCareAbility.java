package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

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
  public void applyEgg(EggBuilder builder) {
    Pokemon male = builder.getMale();
    Pokemon female = builder.getFemale();
    Pokemon firstEvolution = builder.getFirstEvolution();
    boolean hasAh = PokemonUtils.isAH(male) || PokemonUtils.isAH(female);
    boolean getAh = Utils.RANDOM.nextDouble(100) <= percentageTransmitAH;
    boolean notDitto = isDitto(male) || isDitto(female);

    Ability ability;
    if (getAh && hasAh && !notDitto) {
      ability = PokemonUtils.getAH(firstEvolution);
    } else {
      ability = PokemonUtils.getRandomAbility(firstEvolution);
    }
    String data = ability.getName();
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("Ability: " + data + " applied to " + firstEvolution.getSpecies().showdownId());
    }
    builder.getEgg().getPersistentData().putString(TAG, data);
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String ability = egg.getPersistentData().getString(TAG);
    if (!ability.isEmpty()) {
      AbilityTemplate abilityTemplate = Abilities.INSTANCE.get(ability);
      if (abilityTemplate != null) {
        PokemonProperties.Companion.parse("ability=" + abilityTemplate.getName()).apply(egg);
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info("Ability: " + abilityTemplate.getName() + " applied to " + egg.getSpecies().showdownId());
        }
      } else {
        CobbleUtils.LOGGER.error("Ability not found: " + ability);
      }
    }


    egg.getPersistentData().remove(TAG);
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    String ability = PokemonUtils.getAH(pokemon).getName();
    egg.getPersistentData().putString(TAG, ability);
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("Ability: " + ability + " applied to " + pokemon.getSpecies().showdownId());
    }
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "ability";
  }
}
